package com.rateio.app.ui.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.app.ui.format.formatInstantAsShortDate
import com.rateio.domain.engine.DebtSimplificationEngine
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Money
import com.rateio.domain.model.Participant
import com.rateio.domain.model.Settlement
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteGroupRepository
import com.rateio.domain.repository.SettlementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela "Detalhes do grupo" (T42.2, RF42) — o consumidor real de
 * [DebtSimplificationEngine] (T33) na UI: até esta task o motor estava pronto e testado, mas
 * nenhuma tela chamava [DebtSimplificationEngine.computeBalances]. Saldo é sempre recomputado do
 * zero a partir do histórico vigente de despesas + quitações (`algorithm-spec.md`, seção
 * "Regras de negócio" de `computeBalances`) — nunca um contador incremental.
 *
 * Fontes: [GroupRepository] (nome/isSynced do grupo, filtrado de [GroupRepository.getGroupsFlow]
 * porque não existe uma versão reativa de "um grupo só" — mesmo approach que [Group]-por-id já
 * não tinha antes de T42), [ParticipantRepository], [ExpenseRepository] e, desde T42.1,
 * [SettlementRepository] — as quatro em Room, zero rede (constitution.md, princípio 1).
 *
 * [AuthRepository]/[RemoteGroupRepository] entram só para a ação "Sincronizar este grupo" (T19),
 * que T42.4 move do card da lista para cá — mesma lógica que [GroupListViewModel] tinha antes de
 * T42.4, agora escopada a um `groupId` em vez de todos os grupos de uma vez.
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
    private val debtSimplificationEngine: DebtSimplificationEngine,
) : ViewModel() {

    /** Override transiente de [GroupSyncActionUiState] — só guarda InProgress/Failed da última tentativa. */
    private val syncOverride = MutableStateFlow<GroupSyncActionUiState?>(null)

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
     * T19.1/T19.2, reaproveitado: envia grupo + participantes + despesas pro backend; ao sucesso,
     * persiste `isSynced=true` + `remoteId` no Room. Falha de rede nunca chega a mudar o Room —
     * `isSynced` continua `false`, só [syncOverride] vira [GroupSyncActionUiState.Failed].
     */
    fun onSyncGroupClick() {
        if (syncOverride.value is GroupSyncActionUiState.InProgress) return

        viewModelScope.launch {
            syncOverride.value = GroupSyncActionUiState.InProgress
            try {
                syncGroup()
                syncOverride.value = null
            } catch (error: GroupSyncException) {
                syncOverride.value = GroupSyncActionUiState.Failed(error.message ?: "Não foi possível sincronizar.")
            }
        }
    }

    private suspend fun syncGroup() {
        val group = requireNotNull(groupRepository.getGroupById(groupId)) {
            "Grupo $groupId não encontrado pra sincronizar."
        }
        val participants = participantRepository.getParticipantsFlow(groupId).first()
        val expenses = expenseRepository.getExpensesFlow(groupId).first()

        val remoteId = remoteGroupRepository.syncGroup(group, participants, expenses)

        groupRepository.insertGroup(group.copy(isSynced = true, remoteId = remoteId))
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
    payerName = participantNames[paidByParticipantId] ?: "Alguém",
    dateLabel = formatInstantAsShortDate(createdAt),
    amountCents = amountCents,
    splitTypeLabel = splits.splitTypeLabel(),
)

/** "dividido igual" / "dividido por %" / "valor fixo por pessoa" — mesmo texto de `tipoLabel()` em `grupo.html`. */
private fun List<ExpenseSplit>.splitTypeLabel(): String = when (firstOrNull()) {
    is ExpenseSplit.Equal -> "dividido igual"
    is ExpenseSplit.Weight -> "dividido por %"
    is ExpenseSplit.FixedAmount -> "valor fixo por pessoa"
    null -> ""
}

/** Grupo já sincronizado ou usuário deslogado: ação escondida (mesma regra de T19). */
private fun Group.syncActionFor(
    isAuthenticated: Boolean,
    override: GroupSyncActionUiState?,
): GroupSyncActionUiState {
    if (isSynced || !isAuthenticated) return GroupSyncActionUiState.Hidden
    return override ?: GroupSyncActionUiState.Available
}
