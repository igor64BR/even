package com.rateio.app.ui.joingroup

/**
 * Estado da tela "Entrar no grupo" (T22.2, deep link `rateio://join/{codigo}`).
 *
 * Não existe um estado "grupo X" com nome pra mostrar antes de confirmar: o backend (T21) não
 * expõe um endpoint de preview do grupo pelo código, só o de entrar de verdade — por isso
 * [Confirming] carrega só o [inviteCode] usado na chamada, nunca um nome de grupo inventado (ver
 * `T22-app-entrar-via-link.md`, "não invente").
 */
sealed interface JoinGroupUiState {

    /** Sessão ainda não resolvida (primeira emissão de [com.rateio.domain.repository.AuthRepository.getSessionFlow] não chegou). */
    data object CheckingSession : JoinGroupUiState

    /**
     * Deslogado: a ação só faz sentido autenticado (T21 exige `[Authorize]`). Quem observa este
     * estado é responsável por guardar [inviteCode] e mandar pra tela de login, retomando o fluxo
     * depois — a própria tela de confirmação não navega sozinha.
     */
    data class NeedsLogin(val inviteCode: String) : JoinGroupUiState

    /** Logado, aguardando confirmação — "Você foi convidado a entrar em um grupo". */
    data class Confirming(val inviteCode: String) : JoinGroupUiState

    /** Botão "Entrar" tocado, chamada em andamento. */
    data object Joining : JoinGroupUiState

    /**
     * `POST /groups/join/{codigo}` respondeu com sucesso. Só [remoteGroupId] — sem nome,
     * participantes ou despesas (lacuna documentada em [com.rateio.domain.repository.RemoteGroupRepository.joinByCode]),
     * então a UI mostra confirmação genérica, não os detalhes do grupo.
     */
    data class Success(val remoteGroupId: String) : JoinGroupUiState

    /** Falha de rede/HTTP (inclui código inválido/expirado) já traduzida por [com.rateio.domain.repository.GroupSyncException]. */
    data class Error(val inviteCode: String, val message: String) : JoinGroupUiState
}
