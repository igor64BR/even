package com.tally.app.ui.settledebts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.domain.engine.DebtSimplificationEngine
import com.tally.domain.model.Money
import com.tally.domain.model.Settlement
import com.tally.domain.model.SettlementSuggestion
import com.tally.domain.repository.ExpenseRepository
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.ParticipantRepository
import com.tally.domain.repository.SettlementRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State + "Mark as paid" action for the "Settle debts" screen (T42.3, RF44). Recomputes
 * [DebtSimplificationEngine.computeBalances] + [DebtSimplificationEngine.computeSettlement] (T33)
 * every time the group's expenses or settlements change in Room — the suggestion list is never
 * kept as its own state, it's always derived (same principle as
 * [com.tally.app.ui.groupdetail.GroupDetailViewModel]).
 *
 * "Mark as paid" ([onMarkAsPaidClick]) writes a new [Settlement] via [settlementRepository]
 * (T42.1); since [uiState] is combined from [SettlementRepository.getSettlementsFlow], the write
 * alone already triggers the recalculation — there's no "manual reload" here.
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
                    createdAt = Instant.now(),
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
    fromName = participantNames[fromParticipantId] ?: "Someone",
    toName = participantNames[toParticipantId] ?: "Someone",
    amountCents = amount.cents,
)
