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
 *
 * [syncAction] (T19) tem default [GroupSyncActionUiState.Hidden] pra não quebrar quem já
 * construía este model sem se importar com sincronização (previews).
 */
data class GroupListItemUiModel(
    val id: String,
    val name: String,
    val tag: String,
    val participantCount: Int,
    val isSynced: Boolean,
    val balance: GroupBalance,
    val syncAction: GroupSyncActionUiState = GroupSyncActionUiState.Hidden,
)

/**
 * Estado da ação "Sincronizar este grupo" (T19) pra um card específico. Não existe tela de
 * detalhe de grupo ainda (RF42), então a ação mora aqui, no card da lista — ver
 * `GroupCard.SyncAction`.
 */
sealed interface GroupSyncActionUiState {
    /** Grupo já sincronizado, ou usuário não autenticado — não faz sentido oferecer a ação. */
    data object Hidden : GroupSyncActionUiState

    /** Grupo local, usuário autenticado: pode tocar pra sincronizar. */
    data object Available : GroupSyncActionUiState

    /** Chamada em andamento. */
    data object InProgress : GroupSyncActionUiState

    /**
     * Falha de rede/HTTP na última tentativa — [Group.isSynced] continua `false` (nenhum estado
     * inconsistente), [message] já vem pronta pra tela (ver [com.rateio.domain.repository.GroupSyncException]).
     */
    data class Failed(val message: String) : GroupSyncActionUiState
}

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
