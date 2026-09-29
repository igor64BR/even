package com.tally.domain.model

/**
 * A transaction suggested by the debt-simplification engine (`DebtSimplificationEngine`, T33):
 * "[fromParticipantId] should pay [amount] to [toParticipantId]". Mirrors `Transaction`
 * (`backend/src/Tally.Domain/Transaction.cs`).
 *
 * Don't confuse this with [Settlement]: `Settlement` is a settlement already recorded by the user
 * (it has [Settlement.id] and feeds `computeBalances` as input); `SettlementSuggestion` is a
 * calculated output of `computeSettlement` — it has no identity of its own, it's derived from the
 * balance and recomputed from scratch on every call, so carrying a persistent `id` wouldn't make
 * sense.
 */
data class SettlementSuggestion(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Money,
)
