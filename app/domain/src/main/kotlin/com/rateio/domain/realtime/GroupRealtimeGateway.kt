package com.rateio.domain.realtime

/**
 * Conexão em tempo real com os eventos de um grupo sincronizado (T40.1/T40.2, RF35/RF36;
 * constitution.md princípio 3 — sistema próprio via Hub, sem push de terceiros, limitação de
 * kill-state documentada). `:domain` só enxerga "conectar a um grupo" / "desconectar" — SignalR é
 * detalhe de `:data` (única implementação real,
 * `com.rateio.data.remote.realtime.SignalRGroupRealtimeGateway`), pelo mesmo motivo que
 * [com.rateio.domain.repository.AuthRepository]/[com.rateio.domain.repository.GroupRepository] não
 * deixam Retrofit/Room vazar pra cá (Dependency Inversion).
 *
 * Quem chama é responsável por só conectar enquanto a tela do grupo sincronizado está em foco
 * (`GroupDetailScreen`, T42) — esta interface não impõe isso sozinha, pra não acoplar a um ciclo de
 * vida de UI específico.
 */
interface GroupRealtimeGateway {

    /**
     * [localGroupId] é o id local (Room) do grupo — usado por quem implementa pra resolver nome do
     * grupo/participantes na hora de montar a notificação. [remoteGroupId] é o id do grupo no
     * backend (`Group.remoteId`, T19.2) — o que o Hub (`RateioHub.EntrarNoGrupoAsync`) e o fallback
     * de pull (T39, `GET /groups/{id}/events`) esperam. Os dois nunca são o mesmo valor; chamar com
     * um grupo que ainda não tem [remoteGroupId] (não sincronizado) não faz sentido — quem chama
     * garante isso antes (ver `GroupDetailViewModel.startRealtimeUpdates`).
     */
    suspend fun connect(localGroupId: String, remoteGroupId: String)

    /** Idempotente: chamar sem uma conexão ativa não é erro. */
    suspend fun disconnect()
}
