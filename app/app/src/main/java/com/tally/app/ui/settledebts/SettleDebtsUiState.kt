package com.tally.app.ui.settledebts

/**
 * State for the "Settle debts" screen (T42.3, RF44). [SettledUp] is the prototype's "group
 * settled" state (`settle.html`, empty transaction list) — visually and semantically different
 * from [Loading] (we still don't know), so they're two distinct states, not a `Content` with an
 * empty list in disguise.
 */
sealed interface SettleDebtsUiState {
    data object Loading : SettleDebtsUiState
    data class SettledUp(val groupName: String) : SettleDebtsUiState
    data class Content(val groupName: String, val suggestions: List<SettlementSuggestionRowUiModel>) : SettleDebtsUiState
}

/**
 * A transaction suggested by the simplification engine (`DebtSimplificationEngine.computeSettlement`,
 * T33), with the names already resolved for display (the prototype's `.settle-row`: "A → B: $X").
 * No `id` of its own — same reason as [com.tally.domain.model.SettlementSuggestion]: it's derived
 * from the current balance, recomputed from scratch on every change, not a persistent entity.
 */
data class SettlementSuggestionRowUiModel(
    val fromParticipantId: String,
    val toParticipantId: String,
    val fromName: String,
    val toName: String,
    val amountCents: Long,
)
