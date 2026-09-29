package com.tally.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.ui.format.formatCentsAsAmountInput
import com.tally.app.ui.format.parseAmountInputToCents
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Money
import com.tally.domain.model.Participant
import com.tally.domain.repository.ExpenseRepository
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.ParticipantRepository
import com.tally.domain.repository.RemoteExpenseRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State + split calculation + persistence for the "New expense"/"Edit expense" form (T24, extended
 * in T29 for edit mode), for an already-existing [groupId] (local or synced group — doesn't matter
 * here, see the note in [propagateUpdateIfSynced]). Participants come from [participantRepository]
 * (Room, T7B); the Equal split uses [calculateEqualSplit] (a pure function, tested in isolation)
 * instead of naive integer `/`, to close out cents the same way the engine does
 * (`algorithm-spec.md`). Persistence is always local via [expenseRepository] — zero blocking
 * network calls (constitution.md, principle 1).
 *
 * [expenseId] is the only construction difference between the two modes (T29, "editing is state,
 * not a new screen"): `null` creates a new expense (T24), non-null pre-loads that expense from
 * Room ([loadExpenseForEditing]) and makes `onSaveClick` do an update instead of an insert
 * ([updateExistingExpense]). [groupRepository]/[remoteExpenseRepository] only come into play on
 * edit, to propagate the change to the backend when the group is already synced (T29,
 * [propagateUpdateIfSynced]) — T24 never needed this because creating an expense in a synced group
 * is a known gap documented separately (RemoteGroupRepository doesn't cover a standalone expense).
 */
class CreateExpenseViewModel(
    private val groupId: String,
    private val expenseId: String? = null,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val remoteExpenseRepository: RemoteExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExpenseUiState(expenseId = expenseId))
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateExpenseEvent>()
    val events: SharedFlow<CreateExpenseEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val participants = participantRepository.getParticipantsFlow(groupId).first()
            _uiState.update { it.withParticipants(participants) }
            expenseId?.let { loadExpenseForEditing(it) }
        }
    }

    /**
     * T29.1: pre-fills the form with the expense being edited — a one-off read
     * ([ExpenseRepository.getExpenseById]), not the list's `Flow`, because we only read once, when
     * opening the screen (see the KDoc of `ExpenseDao.getExpenseWithSplitsById`). Silently does
     * nothing if the expense no longer exists (deleted from another screen while this one was
     * opening) — the same defense as [com.tally.app.ui.groupdetail.GroupDetailUiState.NotFound].
     */
    private suspend fun loadExpenseForEditing(expenseId: String) {
        val expense = expenseRepository.getExpenseById(expenseId) ?: return
        _uiState.update { it.withExpenseForEditing(expense) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description.take(MAX_DESCRIPTION_LENGTH), descriptionError = false) }
    }

    fun onAmountChanged(amountInput: String) {
        _uiState.update { it.copy(amountInput = amountInput, amountError = false).withRecalculatedSplit() }
    }

    fun onPayerSelected(payerId: String) {
        _uiState.update { it.copy(payerId = payerId) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    /** Checks/unchecks a participant in the split (`data-id` of the checkbox in the prototype). */
    fun onParticipantToggled(participantId: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(isIncluded = !row.isIncluded) else row
                },
            ).withRecalculatedSplit()
        }
    }

    /** Switches the active tab in "How to split" (T26). `participantsError` only makes sense in
     * Equal mode — it clears on tab switch, just like the prototype swaps `#split-area` without
     * carrying over the previous tab's error. */
    fun onSplitModeSelected(mode: SplitMode) {
        _uiState.update { it.copy(splitMode = mode, participantsError = false) }
    }

    /** `data-id` of the `%` input in the Percentage tab (T26.1). */
    fun onPercentageChanged(participantId: String, percentageInput: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(percentageInput = percentageInput) else row
                },
            )
        }
    }

    /** `data-id` of the R$ input in the Fixed amount tab (T26.2). */
    fun onFixedAmountChanged(participantId: String, fixedAmountInput: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(fixedAmountInput = fixedAmountInput) else row
                },
            )
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = state.validate()
        _uiState.update {
            it.copy(
                descriptionError = !validation.descriptionValid,
                amountError = !validation.amountValid,
                // Only the Equal mode uses this flag to paint the participant list as an error — in
                // the other two modes an invalid sum is already visible live in #split-sum, no need
                // for a second warning (see CreateExpenseUiState's class note).
                participantsError = state.splitMode == SplitMode.EQUAL && !validation.splitValid,
            )
        }
        if (!validation.isValid) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val editingId = expenseId
                if (editingId != null) {
                    updateExistingExpense(state, validation.amountCents, editingId)
                } else {
                    insertNewExpense(state, validation.amountCents)
                }
            } finally {
                // Without this, `isSaving` would stay `true` forever — harmless while the screen
                // unmounted when navigating back to the group right after the event below, but it
                // turned into a visible bug (a permanently disabled "Save expense" button) as soon
                // as the same ViewModel was reused on a subsequent visit to this screen (see the
                // key in CreateExpenseRoute). Resets BEFORE emitting the event (not after, in a
                // `finally` only around the emit either): "saving" ends when the expense is
                // persisted, not when someone reacts to the navigation notification — and only this
                // way does whoever observes [events] already see `isSaving = false` at the same
                // instant the event arrives.
                _uiState.update { it.copy(isSaving = false) }
            }
            _events.emit(CreateExpenseEvent.Saved)
        }
    }

    private suspend fun insertNewExpense(state: CreateExpenseUiState, amountCents: Long) {
        val payerId = requireNotNull(state.payerId) { "No payer selected for group $groupId." }
        expenseRepository.insertExpense(
            state.toExpense(id = UUID.randomUUID().toString(), payerId = payerId, amountCents = amountCents),
        )
    }

    /**
     * T29.1: saves the edit to Room (an upsert — [ExpenseRepository.insertExpense] with the same
     * [expenseId] replaces the expense + its splits, see the KDoc of
     * `ExpenseDao.insertWithSplits`) and only then tries to propagate it to the backend, never the
     * other way around — local-first means the edit already counts locally before any network
     * attempt (constitution.md, principle 1).
     */
    private suspend fun updateExistingExpense(state: CreateExpenseUiState, amountCents: Long, editingExpenseId: String) {
        val payerId = requireNotNull(state.payerId) { "No payer selected for group $groupId." }
        val expense = state.toExpense(id = editingExpenseId, payerId = payerId, amountCents = amountCents)

        expenseRepository.insertExpense(expense)
        propagateUpdateIfSynced(expense)
    }

    private fun CreateExpenseUiState.toExpense(id: String, payerId: String, amountCents: Long) = Expense(
        id = id,
        groupId = groupId,
        description = description.trim(),
        amountCents = amountCents,
        paidByParticipantId = payerId,
        createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        splits = buildSplits(splitMode, splitRows),
    )

    /**
     * T29: propagates the edit to the backend when [groupId] is already synced — the same pattern
     * as [com.tally.app.ui.groupdetail.GroupDetailViewModel.onSyncGroupClick] (T19), except
     * best-effort and silent: the local edit already happened in the line above
     * ([updateExistingExpense]), so a network/session failure here never undoes or blocks the
     * [CreateExpenseEvent.Saved] event for the UI (local-first, constitution.md principle 1 —
     * "local change doesn't wait for server confirmation", T29-app-editar-excluir-despesa.md).
     * Unlike "Sync this group" (T19), there's no "retry" action for a single expense today — that's
     * left for when the app gets a real sync queue.
     */
    private suspend fun propagateUpdateIfSynced(expense: Expense) {
        val group = groupRepository.getGroupById(groupId) ?: return
        val remoteId = group.remoteId?.takeIf { group.isSynced } ?: return
        try {
            remoteExpenseRepository.updateExpense(remoteId, expense)
        } catch (error: GroupSyncException) {
            // See the function's KDoc: a network/HTTP failure doesn't undo the local edit.
        }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 40
    }
}

/**
 * Participants freshly loaded from Room: everyone enters the split, the default payer is "You".
 * The default percentage (`Math.round(100 / n)`, [defaultPercentage]) is seeded just like the
 * prototype — the fixed amount stays empty (there's no sensible "default fixed amount" to
 * propose).
 */
private fun CreateExpenseUiState.withParticipants(participants: List<Participant>): CreateExpenseUiState {
    val defaultPayerId = participants.firstOrNull { it.isYou }?.id ?: participants.firstOrNull()?.id
    val defaultPercentageInput = defaultPercentage(participants.size).toString()
    val rows = participants.map { participant ->
        ExpenseSplitRowUiModel(
            participantId = participant.id,
            name = participant.name,
            isYou = participant.isYou,
            isIncluded = true,
            percentageInput = defaultPercentageInput,
        )
    }
    return copy(participants = participants, payerId = defaultPayerId, splitRows = rows).withRecalculatedSplit()
}

/**
 * T29.1: the inverse of [buildSplits] — reconstructs [CreateExpenseUiState.splitMode]/
 * [ExpenseSplitRowUiModel] from [Expense.splits] of an already-existing expense, instead of
 * [withParticipants]'s defaults. Called after [withParticipants] in `init` (same order): the rows
 * already exist with name/isYou/default percentage seeded, here only the fields the recorded
 * expense actually had are overwritten. A participant with no corresponding split (joined the
 * group after the expense was logged) is left out of the split when reopened for editing —
 * faithfully reflecting what was saved, instead of inventing a split that never existed.
 */
private fun CreateExpenseUiState.withExpenseForEditing(expense: Expense): CreateExpenseUiState {
    val splitsByParticipant = expense.splits.associateBy { it.participantId }
    val rows = splitRows.map { row ->
        when (val split = splitsByParticipant[row.participantId]) {
            is ExpenseSplit.Equal -> row.copy(isIncluded = true)
            is ExpenseSplit.Weight -> row.copy(isIncluded = true, percentageInput = split.weight.toString())
            is ExpenseSplit.FixedAmount ->
                row.copy(isIncluded = true, fixedAmountInput = formatCentsAsAmountInput(split.amount.cents))
            null -> row.copy(isIncluded = false, percentageInput = "0", fixedAmountInput = "")
        }
    }
    return copy(
        description = expense.description,
        amountInput = formatCentsAsAmountInput(expense.amountCents),
        payerId = expense.paidByParticipantId,
        date = expense.createdAt.atZone(ZoneId.systemDefault()).toLocalDate(),
        splitMode = expense.splits.toUiSplitMode(),
        splitRows = rows,
    ).withRecalculatedSplit()
}

/** Mirrors [com.tally.app.ui.groupdetail.GroupDetailViewModel]'s `splitTypeLabel()` — derived from the first split. */
private fun List<ExpenseSplit>.toUiSplitMode(): SplitMode = when (firstOrNull()) {
    is ExpenseSplit.Weight -> SplitMode.PERCENTAGE
    is ExpenseSplit.FixedAmount -> SplitMode.FIXED_AMOUNT
    is ExpenseSplit.Equal, null -> SplitMode.EQUAL
}

/** Recalculates [ExpenseSplitRowUiModel.amountCents] for each checked row (T24.2, live calculation). */
private fun CreateExpenseUiState.withRecalculatedSplit(): CreateExpenseUiState {
    val totalCents = parseAmountInputToCents(amountInput) ?: 0L
    val includedIds = splitRows.filter { it.isIncluded }.map { it.participantId }
    val parts = calculateEqualSplit(Money.ofCents(totalCents), includedIds)

    return copy(
        splitRows = splitRows.map { row -> row.copy(amountCents = parts[row.participantId]?.cents ?: 0L) },
    )
}

private data class FormValidation(
    val descriptionValid: Boolean,
    val amountValid: Boolean,
    val splitValid: Boolean,
    val amountCents: Long,
) {
    val isValid: Boolean get() = descriptionValid && amountValid && splitValid
}

/**
 * T24.4 + T26.1/T26.2: description required, amount > 0, and the split needs to add up according
 * to the active tab — at least 1 participant selected in Equal mode, sum = 100% in Percentage (see
 * [isPercentageSplitComplete]), sum = total amount in Fixed amount (see
 * [isFixedAmountSplitComplete]).
 */
private fun CreateExpenseUiState.validate(): FormValidation {
    val amountCents = parseAmountInputToCents(amountInput) ?: 0L
    val splitValid = when (splitMode) {
        SplitMode.EQUAL -> splitRows.any { it.isIncluded }
        SplitMode.PERCENTAGE -> isPercentageSplitComplete(splitRows)
        SplitMode.FIXED_AMOUNT -> isFixedAmountSplitComplete(splitRows, Money.ofCents(amountCents))
    }
    return FormValidation(
        descriptionValid = description.isNotBlank(),
        amountValid = amountCents > 0,
        splitValid = splitValid,
        amountCents = amountCents,
    )
}

/**
 * Translates [rows] into the right [ExpenseSplit] subtype based on [mode] (T26.3: "on save, build
 * Weight/FixedAmount for the selected participants").
 * - Equal: only the checked participants, [ExpenseSplit.Equal] — unchanged since T24.
 * - Percentage: every participant (no checkbox in this tab, same as the prototype), weight = the
 *   typed percentage (0 if the input is empty/invalid — this is only reached if the sum already
 *   added up to 100%, so "empty" shouldn't be left over, but there's no reason to persist `null` as
 *   an invalid state instead of 0).
 * - Fixed amount: only participants with a typed, positive amount (`fixos[p.id] > 0` in the
 *   prototype) — whoever is left at 0/empty doesn't take part in the expense.
 */
private fun buildSplits(mode: SplitMode, rows: List<ExpenseSplitRowUiModel>): List<ExpenseSplit> =
    when (mode) {
        SplitMode.EQUAL -> rows
            .filter { it.isIncluded }
            .map { row -> ExpenseSplit.Equal(participantId = row.participantId) }

        SplitMode.PERCENTAGE -> rows.map { row ->
            ExpenseSplit.Weight(
                participantId = row.participantId,
                weight = parsePercentageInput(row.percentageInput) ?: 0L,
            )
        }

        SplitMode.FIXED_AMOUNT -> rows.mapNotNull { row ->
            val amount = parseFixedAmountInput(row.fixedAmountInput)
            if (amount != null && amount.isPositive) {
                ExpenseSplit.FixedAmount(participantId = row.participantId, amount = amount)
            } else {
                null
            }
        }
    }
