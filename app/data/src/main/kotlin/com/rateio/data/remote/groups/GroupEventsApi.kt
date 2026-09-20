package com.rateio.data.remote.groups

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelha `GET /groups/{id}/events?desde=` (T39.1 — fallback de pull, RF35/RF36; constitution.md
 * princípio 3: sem push de terceiros, essa é a forma real de recuperar o que a conexão em tempo
 * real perdeu). T39 rodou em paralelo com T40/T41; este cliente foi escrito contra o contrato
 * documentado na task dele antes do endpoint existir de verdade, e depois conferido linha a linha
 * contra `GruposController.ObterEventos`/`ObterEventosDeGrupoUseCase` (T39.1, já mergeado em
 * `master`, commit "feat(api): endpoint de fallback de pull de eventos (T39.1)") — sem precisar de
 * ajuste: o backend devolve `IReadOnlyList<object>.Cast<object>()` a partir de
 * `EventoDespesaCriada`/`EventoDividaQuitada` concretos (não a interface `IEventoDeGrupo`,
 * justamente pra `System.Text.Json` serializar os campos de cada tipo, não só os da interface) —
 * cada item da lista já sai com exatamente os campos do respectivo evento (`tipo` inclusive, o
 * `TipoEventoDeGrupo` computado da interface).
 *
 * [GrupoEventoDto] modela essa lista heterogênea como um DTO único "largo" (todos os campos das
 * duas variantes concretas, `null` pro que não se aplica a cada item), discriminado por
 * [GrupoEventoDto.tipo] — kotlinx.serialization com campos default cobre um item que só tem o
 * subconjunto de campos do seu tipo sem precisar de um `sealed`/polimorfismo à parte no client.
 * `tipo` serializa como `Int` (mesma convenção de enum sem `JsonStringEnumConverter` já documentada
 * em `RemoteGroupSyncRepository`).
 *
 * **Lacuna real, confirmada no código do backend** (não mais suposição): nem `EventoDespesaCriada`
 * nem `EventoDividaQuitada` carregam timestamp — `ObterEventosDeGrupoUseCase` usa `CriadoEm` só
 * pra ordenar/filtrar no servidor e descarta o campo antes de montar o evento de resposta
 * (`.Select(item => item.Evento)`). `com.rateio.data.remote.realtime.MissedGroupEventsSynchronizer`
 * usa o horário local do aparelho no momento em que processa cada evento como
 * `GroupNotification.occurredAt`, tanto pra este endpoint quanto pros eventos em tempo real (mesma
 * limitação nos dois casos: nenhum envelope carrega o instante em que o evento realmente ocorreu
 * no servidor).
 *
 * `desde` (`[FromQuery] DateTimeOffset desde`, sem valor default no backend) é sempre obrigatório
 * — por isso [getEvents] recebe `since: String` não-nulo; `MissedGroupEventsSynchronizer` manda
 * `Instant.EPOCH` quando ainda não há nenhuma notificação local conhecida, nunca omite o parâmetro.
 */
interface GroupEventsApi {
    @GET("groups/{id}/events")
    suspend fun getEvents(
        @Header("Authorization") bearerToken: String,
        @Path("id") groupId: String,
        @Query("desde") since: String,
    ): List<GrupoEventoDto>
}

/** Ver [GroupEventsApi] — DTO único cobrindo `EventoDespesaCriada`/`EventoDividaQuitada`, discriminado por [tipo]. */
@Serializable
data class GrupoEventoDto(
    val tipo: Int,
    val grupoId: String,
    val despesaId: String? = null,
    val quitacaoId: String? = null,
    val descricao: String? = null,
    val valorTotalCentavos: Long? = null,
    val pagadorId: String? = null,
    val deParticipanteId: String? = null,
    val paraParticipanteId: String? = null,
    val valorCentavos: Long? = null,
)

/** Espelha `TipoEventoDeGrupo.DespesaCriada` (valor ordinal 0) do backend. */
const val TIPO_EVENTO_DESPESA_CRIADA = 0

/** Espelha `TipoEventoDeGrupo.DividaQuitada` (valor ordinal 1) do backend. */
const val TIPO_EVENTO_DIVIDA_QUITADA = 1
