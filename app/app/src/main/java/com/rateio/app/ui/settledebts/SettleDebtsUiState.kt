package com.rateio.app.ui.settledebts

/**
 * Estado da tela "Quitar dívidas" (T42.3, RF44). [SettledUp] é o estado "grupo quitado" do
 * protótipo (`quitar.html`, lista de transações vazia) — visualmente e semanticamente diferente de
 * [Loading] (ainda não sabemos), então são dois estados distintos, não um `Content` com lista
 * vazia disfarçado.
 */
sealed interface SettleDebtsUiState {
    data object Loading : SettleDebtsUiState
    data class SettledUp(val groupName: String) : SettleDebtsUiState
    data class Content(val groupName: String, val suggestions: List<SettlementSuggestionRowUiModel>) : SettleDebtsUiState
}

/**
 * Uma transação sugerida pelo motor de simplificação (`DebtSimplificationEngine.computeSettlement`,
 * T33), já com os nomes resolvidos pra exibição (`.settle-row` do protótipo: "A → B: R$X"). Sem
 * `id` próprio — mesma razão de [com.rateio.domain.model.SettlementSuggestion]: é derivada do
 * saldo atual, recalculada do zero a cada mudança, não uma entidade persistente.
 */
data class SettlementSuggestionRowUiModel(
    val fromParticipantId: String,
    val toParticipantId: String,
    val fromName: String,
    val toName: String,
    val amountCents: Long,
)
