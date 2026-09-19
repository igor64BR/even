package com.rateio.app.ui.groups

/** Estado da tela "Seus grupos" (RF40/RF41) — carregando, sem grupos, ou lista carregada. */
sealed interface GroupListUiState {
    data object Loading : GroupListUiState
    data object Empty : GroupListUiState
    data class Content(val groups: List<GroupListItemUiModel>) : GroupListUiState
}

/**
 * Um card da lista. Réplica do que `index.html` monta por grupo: sigla do grupo (`group-tag`),
 * nome, contagem de participantes + ícone de sincronização, e saldo colorido.
 */
data class GroupListItemUiModel(
    val id: String,
    val name: String,
    val tag: String,
    val participantCount: Int,
    val isSynced: Boolean,
    val balance: GroupBalance,
)

/**
 * Saldo do usuário no grupo, já na semântica de exibição do protótipo
 * (`balanceInfo()` em `index.html`): "te devem" (verde/credit), "você deve" (terracota/owed) ou
 * "quitado" (neutro).
 */
sealed interface GroupBalance {
    data object Settled : GroupBalance
    data class YouAreOwed(val amountCents: Long) : GroupBalance
    data class YouOwe(val amountCents: Long) : GroupBalance
}
