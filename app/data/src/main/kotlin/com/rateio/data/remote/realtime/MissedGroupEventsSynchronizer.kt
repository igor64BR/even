package com.rateio.data.remote.realtime

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.domain.repository.NotificationRepository
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import retrofit2.HttpException

/**
 * T39 (fallback de pull, RF35/RF36; constitution.md princípio 3 — sem push de terceiros): busca
 * eventos perdidos desde a última notificação local conhecida e grava cada um via
 * [GroupEventRecorder]. [SignalRGroupRealtimeGateway] chama [sync] tanto na primeira conexão bem-
 * sucedida quanto em toda reconexão — cobre "rede caiu e voltou" (T40.2) e também "app foi fechado
 * e reaberto" (sem esse segundo caso, um evento perdido enquanto o app estava totalmente fechado
 * nunca apareceria: não há reconexão nenhuma pra disparar o pull nesse cenário — mesma limitação
 * de kill-state que constitution.md princípio 3 já documenta como trade-off aceito).
 *
 * Extraída de [SignalRGroupRealtimeGateway] pra ser testável sem `HubConnection` nenhum
 * (`MissedGroupEventsSynchronizerTest`, com um [GroupEventsApi] dublê — mesmo padrão de
 * `RemoteGroupSyncRepositoryTest`): [SignalRGroupRealtimeGateway] só decide QUANDO chamar isso,
 * esta classe decide O QUE fazer quando chamada.
 *
 * [since]: `GruposController.ObterEventos` (T39.1, já mergeado — `[FromQuery] DateTimeOffset
 * desde` sem default) exige o parâmetro sempre presente, então nunca omitimos `desde` — sem
 * nenhuma notificação local ainda ([NotificationRepository.getLastEventTimestamp] `null`), pedimos
 * desde [Instant.EPOCH] (equivalente a "todo o histórico"), nunca omitindo o parâmetro (isso
 * daria 400 do model binding do ASP.NET Core antes até de chegar no use case).
 *
 * Nunca propaga falha pro chamador: falha de rede não pode derrubar a conexão em tempo real por
 * causa disso.
 */
internal class MissedGroupEventsSynchronizer(
    private val groupEventsApi: GroupEventsApi,
    private val tokenStorage: TokenStorage,
    private val notificationRepository: NotificationRepository,
    private val eventRecorder: GroupEventRecorder,
) {
    suspend fun sync(localGroupId: String, remoteGroupId: String) {
        val accessToken = tokenStorage.read()?.accessToken ?: return

        try {
            val since = notificationRepository.getLastEventTimestamp() ?: Instant.EPOCH
            val events = groupEventsApi.getEvents(
                bearerToken = "Bearer $accessToken",
                groupId = remoteGroupId,
                since = ISO_INSTANT_FORMATTER.format(since),
            )
            events.mapNotNull { it.toDomainEvent() }.forEach { event ->
                eventRecorder.record(localGroupId, event)
            }
        } catch (error: IOException) {
            // Sem conexão com o backend — a próxima reconexão bem-sucedida tenta de novo.
        } catch (error: HttpException) {
            // RNF07 negou acesso (403) ou o grupo não existe mais no servidor (404) — mesmo
            // racional: nunca derruba a conexão em tempo real por causa disso.
        }
    }

    private companion object {
        val ISO_INSTANT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    }
}
