package com.rateio.app.ui.groupdetail

/**
 * Estado da tela "Detalhes do grupo" (T42.2, RF42). [Content] é o único estado com dado real —
 * [Loading] cobre o instante antes do primeiro valor combinado chegar (Room + engine, T33) e
 * [NotFound] cobre um `groupId` que não existe mais no Room (grupo apagado em outra aba/tela
 * enquanto esta estava aberta; não deveria acontecer no fluxo normal, mas evita crash em vez de
 * assumir que o grupo sempre existe).
 */
sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data object NotFound : GroupDetailUiState

    data class Content(
        val groupName: String,
        val participantCount: Int,
        val isSynced: Boolean,
        val balances: List<ParticipantBalanceUiModel>,
        val expenses: List<ExpenseRowUiModel>,
        val syncAction: GroupSyncActionUiState,
    ) : GroupDetailUiState
}

/**
 * Uma linha de "Saldos" (`.split-row` do protótipo `grupo.html`): saldo de um participante,
 * calculado por [com.rateio.domain.engine.DebtSimplificationEngine.computeBalances] (T33) — nunca
 * recalculado aqui, `ParticipantBalanceRow` só apresenta [balance] já pronto.
 */
data class ParticipantBalanceUiModel(
    val participantId: String,
    val name: String,
    val isYou: Boolean,
    val balance: ParticipantBalance,
)

/**
 * Saldo de UM participante do grupo, semântica genérica ("recebe"/"deve", `grupo.html` ->
 * `saldosHtml`) — não confundir com [com.rateio.app.ui.groups.GroupBalance], que é sempre sob a
 * perspectiva de "você" (usado só no card da lista). Aqui qualquer participante pode estar em
 * qualquer um dos três estados, o dono do aparelho incluso.
 */
sealed interface ParticipantBalance {
    data object Settled : ParticipantBalance
    data class Owed(val amountCents: Long) : ParticipantBalance
    data class Credit(val amountCents: Long) : ParticipantBalance
}

/**
 * Uma linha de "Despesas" (`.expense-row` do protótipo): descrição, quem pagou, data curta, tipo
 * de divisão e valor total — nenhum cálculo de divisão mora aqui, [splitTypeLabel] só traduz o
 * subtipo de `ExpenseSplit` (T7B) já escolhido quando a despesa foi lançada (T24).
 */
data class ExpenseRowUiModel(
    val id: String,
    val description: String,
    val payerName: String,
    val dateLabel: String,
    val amountCents: Long,
    val splitTypeLabel: String,
)

/**
 * Estado da ação "Sincronizar este grupo" (T19) para a tela de detalhe. Até T19/antes de T42.4
 * essa ação morava no card da lista de grupos (`com.rateio.app.ui.groups.GroupSyncActionUiState`),
 * atalho temporário porque esta tela ainda não existia. T42.4 move a ação pra cá — é o lugar certo
 * agora que "Detalhes do grupo" existe.
 */
sealed interface GroupSyncActionUiState {
    /** Grupo já sincronizado, ou usuário não autenticado — não faz sentido oferecer a ação. */
    data object Hidden : GroupSyncActionUiState

    /** Grupo local, usuário autenticado: pode tocar pra sincronizar. */
    data object Available : GroupSyncActionUiState

    /** Chamada em andamento. */
    data object InProgress : GroupSyncActionUiState

    /**
     * Falha de rede/HTTP na última tentativa — `Group.isSynced` continua `false` (nenhum estado
     * inconsistente), [message] já vem pronta pra tela (ver
     * [com.rateio.domain.repository.GroupSyncException]).
     */
    data class Failed(val message: String) : GroupSyncActionUiState
}
