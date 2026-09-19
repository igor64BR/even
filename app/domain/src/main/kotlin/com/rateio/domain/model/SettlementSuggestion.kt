package com.rateio.domain.model

/**
 * Uma transação sugerida pelo motor de simplificação de dívidas (`DebtSimplificationEngine`,
 * T33): "[fromParticipantId] deve pagar [amount] para [toParticipantId]". Espelha `Transacao`
 * (`backend/src/Rateio.Domain/Transacao.cs`).
 *
 * Não confundir com [Settlement]: `Settlement` é uma quitação já registrada pelo usuário (tem
 * [Settlement.id] e alimenta `computeBalances` como entrada); `SettlementSuggestion` é uma saída
 * calculada de `computeSettlement` — não tem identidade própria, é derivada do saldo e recalculada
 * do zero a cada chamada, então não faz sentido carregar um `id` persistente.
 */
data class SettlementSuggestion(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Money,
)
