package com.tally.app.ui.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.ui.format.formatInstantAsShortDate
import com.tally.domain.engine.DebtSimplificationEngine
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Group
import com.tally.domain.model.Money
import com.tally.domain.model.Participant
import com.tally.domain.model.Settlement
import com.tally.domain.realtime.GroupRealtimeGateway
import com.tally.domain.repository.AuthRepository
import com.tally.domain.repository.ExpenseRepository
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.ParticipantRepository
import com.tally.domain.repository.RemoteExpenseRepository
import com.tally.domain.repository.RemoteGroupRepository
import com.tally.domain.repository.SettlementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State of the "Group details" screen (T42.2, RF42) — the actual UI consumer of
 * [DebtSimplificationEngine] (T33): before this task the engine was ready and tested, but no
 * screen called [DebtSimplificationEngine.computeBalances]. The balance is always recomputed from
 * scratch from the current history of expenses + settlements (`algorithm-spec.md`,
 * `computeBalances`'s "Business rules" section) — never an incremental counter.
 *
 * Sources: [GroupRepository] (the group's name/isSynced, filtered from
 * [GroupRepository.getGroupsFlow] because there's no reactive "single group" version — the same
 * approach [Group]-by-id already lacked before T42), [ParticipantRepository], [ExpenseRepository]
 * and, since T42.1, [SettlementRepository] — all four in Room, zero network (constitution.md,
 * principle 1).
 *
 * [AuthRepository]/[RemoteGroupRepository] only come in for the "Sync this group" action (T19),
 * which T42.4 moves here from the list card — the same logic [GroupListViewModel] had before
 * T42.4, now scoped to a single `groupId` instead of every group at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
    private val remoteExpenseRepository: RemoteExpenseRepository,
    private val debtSimplificationEngine: DebtSimplificationEngine,
    private val groupRealtimeGateway: GroupRealtimeGateway,
) : ViewModel() {

    /** Transient override for [GroupSyncActionUiState] — only holds InProgress/Failed from the last attempt. */
    private val syncOverride = MutableStateFlow<GroupSyncActionUiState?>(null)

    private var realtimeJob: Job? = null

    val uiState: StateFlow<GroupDetailUiState> = combine(
        groupRepository.getGroupsFlow().map { groups -> groups.firstOrNull { it.id == groupId } },
        participantRepository.getParticipantsFlow(groupId),
        expenseRepository.getExpensesFlow(groupId),
        settlementRepository.getSettlementsFlow(groupId),
        combine(authRepository.getSessionFlow(), syncOverride) { session, override -> (session != null) to override },
    ) { group, participants, expenses, settlements, authState ->
        toUiState(group, participants, expenses, settlements, authState.first, authState.second)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = GroupDetailUiState.Loading,
    )

    /**
     * T19.1/T19.2, reused: sends group + participants + expenses to the backend; on success,
     * persists `isSynced=true` + `remoteId` to Room. A network failure never changes Room —
     * `isSynced` stays `false`, only [syncOverride] becomes [GroupSyncActionUiState.Failed].
     */
    fun onSyncGroupClick() {
        if (syncOverride.value is GroupSyncActionUiState.InProgress) return

        viewModelScope.launch {
            syncOverride.value = GroupSyncActionUiState.InProgress
            try {
                syncGroup()
                syncOverride.value = null
            } catch (error: GroupSyncException) {
                syncOverride.value = GroupSyncActionUiState.Failed(error.message ?: "Couldn't sync.")
            }
        }
    }

    private suspend fun syncGroup() {
        val group = requireNotNull(groupRepository.getGroupById(groupId)) {
            "Group $groupId not found to sync."
        }
        val participants = participantRepository.getParticipantsFlow(groupId).first()
        val expenses = expenseRepository.getExpensesFlow(groupId).first()

        val remoteId = remoteGroupRepository.syncGroup(group, participants, expenses)

        groupRepository.insertGroup(group.copy(isSynced = true, remoteId = remoteId))
    }

    /**
     * T29.2: deletes the expense locally (Room) — called only after the dialog's confirmation
     * ([GroupDetailScreen]) already happened, this function is never the "tap to delete" trigger
     * itself. `uiState` reflects the deletion on its own, via the same combined `Flow` that already
     * recomputes the balance on every change in [expenseRepository] (T33/T42.2) — no extra state
     * needed here.
     */
    fun onDeleteExpenseClick(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
            propagateDeleteIfSynced(expenseId)
        }
    }

    /**
     * T29: propagates the deletion to the backend when the group is already synced — the same
     * rationale as
     * [com.tally.app.ui.createexpense.CreateExpenseViewModel.propagateUpdateIfSynced]: the local
     * deletion already happened on the line above, best-effort and silent, never undoes the local
     * deletion nor blocks the UI waiting for server confirmation (constitution.md, principle 1).
     */
    private suspend fun propagateDeleteIfSynced(expenseId: String) {
        val group = groupRepository.getGroupById(groupId) ?: return
        val remoteId = group.remoteId?.takeIf { group.isSynced } ?: return
        try {
            remoteExpenseRepository.deleteExpense(remoteId, expenseId)
        } catch (error: GroupSyncException) {
            // See the function's KDoc: a network/HTTP failure doesn't undo the local deletion.
        }
    }

    /**
     * T40.1: connects the [groupRealtimeGateway] while this group is synced and the user is
     * authenticated — disconnects as soon as either condition stops holding (group not synced yet,
     * session ended) and reconnects if it holds again. Called explicitly by [GroupDetailRoute] via
     * `DisposableEffect` (not automatically in `init`) because the screen, not the `ViewModel`,
     * knows when it's actually in the foreground — the same care constitution.md principle 3 asks
     * for: "don't connect the Hub globally, only when a synced group is being viewed".
     *
     * Idempotent: calling again with a job already running does nothing.
     */
    fun startRealtimeUpdates() {
        if (realtimeJob != null) return

        realtimeJob = viewModelScope.launch {
            syncedGroupWhileAuthenticatedFlow().collectLatestConnection()
        }
    }

    /** `null` whenever the group isn't synced or there's no session — [startRealtimeUpdates]'s two conditions. */
    private fun syncedGroupWhileAuthenticatedFlow(): Flow<Group?> = combine(
        groupRepository.getGroupsFlow().map { groups -> groups.firstOrNull { it.id == groupId } },
        authRepository.getSessionFlow(),
    ) { group, session -> group.takeIf { it?.isSynced == true && session != null } }.distinctUntilChanged()

    /** Ends the realtime connection, if there's one active — called by [GroupDetailRoute] and by [onCleared]. */
    fun stopRealtimeUpdates() {
        realtimeJob?.cancel()
        realtimeJob = null
    }

    override fun onCleared() {
        stopRealtimeUpdates()
    }

    /**
     * Keeps at most one realtime connection active at a time: every new emission of "should be
     * connected to this group" cancels the previous one (via `collectLatest`) and connects again;
     * `null` (group not synced or no session) just disconnects and waits for the next emission.
     * `remoteId` is always non-null here: [com.tally.domain.model.Group.isSynced] and
     * [com.tally.domain.model.Group.remoteId] transition together (see the KDoc of
     * `Group.remoteId`).
     */
    private suspend fun Flow<Group?>.collectLatestConnection() = collectLatest { group ->
        val remoteId = group?.remoteId ?: return@collectLatest
        try {
            groupRealtimeGateway.connect(localGroupId = group.id, remoteGroupId = remoteId)
            awaitCancellation()
        } finally {
            groupRealtimeGateway.disconnect()
        }
    }

    private fun toUiState(
        group: Group?,
        participants: List<Participant>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        isAuthenticated: Boolean,
        syncOverride: GroupSyncActionUiState?,
    ): GroupDetailUiState {
        if (group == null) return GroupDetailUiState.NotFound

        val balances = debtSimplificationEngine.computeBalances(expenses, settlements)
        val participantNames = participants.associate { it.id to it.name }

        return GroupDetailUiState.Content(
            groupName = group.name,
            participantCount = participants.size,
            isSynced = group.isSynced,
            balances = participants.map { participant -> participant.toBalanceUiModel(balances[participant.id] ?: Money.ZERO) },
            expenses = expenses
                .sortedByDescending { it.createdAt }
                .map { expense -> expense.toRowUiModel(participantNames) },
            settlements = settlements
                .sortedByDescending { it.createdAt }
                .map { settlement -> settlement.toRowUiModel(participantNames) },
            syncAction = group.syncActionFor(isAuthenticated, syncOverride),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Participant.toBalanceUiModel(netBalance: Money) = ParticipantBalanceUiModel(
    participantId = id,
    name = name,
    isYou = isYou,
    balance = when {
        netBalance.isPositive -> ParticipantBalance.Credit(netBalance.cents)
        netBalance.isNegative -> ParticipantBalance.Owed(-netBalance.cents)
        else -> ParticipantBalance.Settled
    },
)

private fun Expense.toRowUiModel(participantNames: Map<String, String>) = ExpenseRowUiModel(
    id = id,
    description = description,
    payerName = participantNames[paidByParticipantId] ?: "Someone",
    dateLabel = formatInstantAsShortDate(createdAt),
    amountCents = amountCents,
    splitTypeLabel = splits.splitTypeLabel(),
)

private fun Settlement.toRowUiModel(participantNames: Map<String, String>) = SettlementRowUiModel(
    id = id,
    payerName = participantNames[payerId] ?: "Someone",
    receiverName = participantNames[receiverId] ?: "Someone",
    dateLabel = formatInstantAsShortDate(createdAt),
    amountCents = amount.cents,
)

/** "split equally" / "split by %" / "fixed amount per person" — the same text as `tipoLabel()` in `group.html`. */
private fun List<ExpenseSplit>.splitTypeLabel(): String = when (firstOrNull()) {
    is ExpenseSplit.Equal -> "split equally"
    is ExpenseSplit.Weight -> "split by %"
    is ExpenseSplit.FixedAmount -> "fixed amount per person"
    null -> ""
}

/** An already-synced group or a signed-out user: the action is hidden (the same rule as T19). */
private fun Group.syncActionFor(
    isAuthenticated: Boolean,
    override: GroupSyncActionUiState?,
): GroupSyncActionUiState {
    if (isSynced || !isAuthenticated) return GroupSyncActionUiState.Hidden
    return override ?: GroupSyncActionUiState.Available
}
