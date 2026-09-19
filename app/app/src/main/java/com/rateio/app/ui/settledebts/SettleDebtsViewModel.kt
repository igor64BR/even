package com.rateio.app.ui.settledebts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.engine.DebtSimplificationEngine
import com.rateio.domain.model.Money
import com.rateio.domain.model.Settlement
import com.rateio.domain.model.SettlementSuggestion
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.SettlementRepository
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado + ação de "Marcar como pago" da tela "Quitar dívidas" (T42.3, RF44). Recalcula
 * [DebtSimplificationEngine.computeBalances] + [DebtSimplificationEngine.computeSettlement] (T33)
 * toda vez que despesas ou quitações do grupo mudam no Room — nunca guarda a lista de sugestões
 * como estado próprio, ela é sempre derivada (mesmo princípio de
 * [com.rateio.app.ui.groupdetail.GroupDetailViewModel]).
 *
 * "Marcar como pago" ([onMarkAsPaidClick]) grava um [Settlement] novo via [settlementRepository]
 * (T42.1); como [uiState] é combinado a partir de [SettlementRepository.getSettlementsFlow], a
 * gravação por si só já dispara o recálculo — não existe um "recarregar manual" aqui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettleDebtsViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val debtSimplificationEngine: DebtSimplificationEngine,
) : ViewModel() {

    val uiState: StateFlow<SettleDebtsUiState> = combine(
        groupRepository.getGroupsFlow().map { groups -> groups.firstOrNull { it.id == groupId }?.name ?: "" },
        participantRepository.getParticipantsFlow(groupId),
        expenseRepository.getExpensesFlow(groupId),
        settlementRepository.getSettlementsFlow(groupId),
    ) { groupName, participants, expenses, settlements ->
        val balances = debtSimplificationEngine.computeBalances(expenses, settlements)
        val suggestions = debtSimplificationEngine.computeSettlement(balances)
        val participantNames = participants.associate { it.id to it.name }

        if (suggestions.isEmpty()) {
            SettleDebtsUiState.SettledUp(groupName)
        } else {
            SettleDebtsUiState.Content(
                groupName = groupName,
                suggestions = suggestions.map { suggestion -> suggestion.toRowUiModel(participantNames) },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettleDebtsUiState.Loading,
    )

    fun onMarkAsPaidClick(suggestion: SettlementSuggestionRowUiModel) {
        viewModelScope.launch {
            settlementRepository.insertSettlement(
                Settlement(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    payerId = suggestion.fromParticipantId,
                    receiverId = suggestion.toParticipantId,
                    amount = Money.ofCents(suggestion.amountCents),
                ),
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun SettlementSuggestion.toRowUiModel(
    participantNames: Map<String, String>,
) = SettlementSuggestionRowUiModel(
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    fromName = participantNames[fromParticipantId] ?: "Alguém",
    toName = participantNames[toParticipantId] ?: "Alguém",
    amountCents = amount.cents,
)
