package com.rateio.data.remote.realtime

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.domain.format.MoneyFormatter
import com.rateio.domain.realtime.GroupRealtimeGateway
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementação real de [GroupRealtimeGateway] (T40.1/T40.2) sobre o client Java oficial do
 * SignalR (`com.microsoft.signalr:signalr`). Conecta a [hubUrl] (`/hubs/rateio`, ver
 * `RotaDoHubDeNotificacoes` no backend), autenticado via access token na query string
 * (`withAccessTokenProvider` — mesmo mecanismo que o `OnMessageReceived` do backend lê pra
 * conexões de Hub, já que WebSocket não manda header `Authorization` no handshake; ver KDoc de
 * `RateioHub`), entra no grupo SignalR do grupo remoto (`EntrarNoGrupoAsync`) e escuta
 * `DespesaCriada`/`DividaQuitada` — os nomes exatos que `NotificadorDeEventoDeGrupoSignalR` usa
 * pra mandar `nameof(TipoEventoDeGrupo.DespesaCriada/DividaQuitada)`.
 *
 * **Reconexão é manual, não da biblioteca**: ao contrário dos clients JS/.NET do SignalR, o client
 * Java oficial não expõe `withAutomaticReconnect`/`onReconnected` (conferido na API pública de
 * `HubConnectionBuilder`/`HubConnection` — só existe `onClosed`; é uma lacuna conhecida do client
 * Java, não uma omissão deste código). [maintainConnection] implementa o retry manualmente: a cada
 * `onClosed`, espera um backoff (mesma progressão documentada do client JS — 0s, 2s, 10s, 30s,
 * repete 30s daí em diante) e reconecta. Toda conexão bem-sucedida — a primeira e cada reconexão —
 * dispara [MissedGroupEventsSynchronizer.sync] (T40.2), o que também cobre "app foi fechado e
 * reaberto" além de "rede caiu e voltou" (ver KDoc de [MissedGroupEventsSynchronizer]).
 *
 * **Sem teste de unidade real de conexão de Hub** (limitação documentada na entrega de T40/T41):
 * esta classe é a única peça do fluxo que efetivamente abre um socket, e não há como testá-la sem
 * um Hub de verdade rodando (Robolectric não sobe um servidor ASP.NET, e adicionar um framework de
 * mock de rede pra isso estaria fora do escopo desta task). Toda a lógica de negócio que importa —
 * montar o texto da notificação, decidir o que persistir, buscar eventos perdidos — foi extraída
 * pra [GroupEventNotificationBuilder], [GroupEventRecorder] e [MissedGroupEventsSynchronizer], as
 * três testadas isoladamente com dublês, sem SignalR nenhum. Esta classe foi verificada manualmente
 * (leitura cuidadosa da API pública do client, ver javadocs de `HubConnection`/
 * `HubConnectionBuilder`) e por inspeção do POM publicado do artefato (RxJava3 + Gson — não RxJava2
 * nem Jackson, como uma leitura desatualizada da documentação sugeriria).
 *
 * Um único grupo por vez: [connect] sempre encerra uma conexão anterior antes de abrir a nova
 * (nunca duas conexões simultâneas) — consistente com "só conecta quando há um grupo sincronizado
 * sendo visualizado" (T40, não é uma feature de multi-grupo).
 *
 * Constrói [GroupEventNotificationBuilder]/[GroupEventRecorder]/[MissedGroupEventsSynchronizer]
 * internamente a partir das dependências "cruas" (Room/Retrofit/[MoneyFormatter]) em vez de
 * recebê-las prontas: as três são `internal` a este módulo (só usadas por esta classe e testadas
 * direto por `:data`), e [com.rateio.app.di.AppContainer], em `:app`, não deveria conhecer esses
 * detalhes de composição — só o suficiente pra montar o gateway como um todo.
 */
class SignalRGroupRealtimeGateway(
    private val hubUrl: String,
    private val tokenStorage: TokenStorage,
    notificationRepository: NotificationRepository,
    groupRepository: GroupRepository,
    participantRepository: ParticipantRepository,
    groupEventsApi: GroupEventsApi,
    moneyFormatter: MoneyFormatter,
) : GroupRealtimeGateway {

    private val eventRecorder = GroupEventRecorder(
        notificationRepository = notificationRepository,
        groupRepository = groupRepository,
        participantRepository = participantRepository,
        notificationBuilder = GroupEventNotificationBuilder(moneyFormatter),
    )
    private val missedEventsSynchronizer = MissedGroupEventsSynchronizer(
        groupEventsApi = groupEventsApi,
        tokenStorage = tokenStorage,
        notificationRepository = notificationRepository,
        eventRecorder = eventRecorder,
    )

    @Volatile
    private var hubConnection: HubConnection? = null
    private var connectionScope: CoroutineScope? = null

    override suspend fun connect(localGroupId: String, remoteGroupId: String) {
        disconnect()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        connectionScope = scope
        scope.launch { maintainConnection(scope, localGroupId, remoteGroupId) }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        connectionScope?.cancel()
        connectionScope = null
        val connection = hubConnection ?: return@withContext
        hubConnection = null
        runCatching { connection.stop().blockingAwait() }
        runCatching { connection.close() }
        Unit
    }

    /**
     * Laço único (sem recursão) que mantém a conexão viva: conecta, aguarda o fechamento
     * ([CompletableDeferred] completado pelo `onClosed` da conexão), reconecta com backoff. Roda
     * inteiro em [scope] — cancelar [scope] (via [disconnect]) interrompe o laço no próximo ponto
     * de suspensão.
     */
    private suspend fun maintainConnection(scope: CoroutineScope, localGroupId: String, remoteGroupId: String) {
        var attempt = 0
        while (scope.isActive) {
            if (attempt > 0) delay(reconnectDelayMillisFor(attempt))

            val accessToken = tokenStorage.read()?.accessToken ?: return
            val connection = buildConnection(accessToken, scope, localGroupId)
            val closed = CompletableDeferred<Unit>()
            connection.onClosed { closed.complete(Unit) }

            val connected = runCatching {
                connection.start().blockingAwait()
                connection.invoke("EntrarNoGrupoAsync", arrayOf<Any>(remoteGroupId)).blockingAwait()
            }.isSuccess

            if (!connected) {
                runCatching { connection.close() }
                attempt += 1
                continue
            }

            hubConnection = connection
            attempt = 0
            missedEventsSynchronizer.sync(localGroupId, remoteGroupId)

            closed.await()
            if (hubConnection === connection) hubConnection = null
            attempt = 1
        }
    }

    private fun buildConnection(accessToken: String, scope: CoroutineScope, localGroupId: String): HubConnection {
        val connection = HubConnectionBuilder.create(hubUrl)
            .withAccessTokenProvider(Single.just(accessToken))
            .build()

        connection.on(
            EVENTO_DESPESA_CRIADA,
            { payload: DespesaCriadaPayload -> recordEvent(scope, localGroupId, payload.toDomainEvent()) },
            DespesaCriadaPayload::class.java,
        )
        connection.on(
            EVENTO_DIVIDA_QUITADA,
            { payload: DividaQuitadaPayload -> recordEvent(scope, localGroupId, payload.toDomainEvent()) },
            DividaQuitadaPayload::class.java,
        )
        return connection
    }

    private fun recordEvent(scope: CoroutineScope, localGroupId: String, event: GroupRealtimeEvent) {
        scope.launch { eventRecorder.record(localGroupId, event) }
    }

    private fun reconnectDelayMillisFor(attempt: Int): Long =
        RECONNECT_DELAYS_MILLIS.getOrElse(attempt - 1) { RECONNECT_DELAYS_MILLIS.last() }

    private companion object {
        const val EVENTO_DESPESA_CRIADA = "DespesaCriada"
        const val EVENTO_DIVIDA_QUITADA = "DividaQuitada"
        val RECONNECT_DELAYS_MILLIS = longArrayOf(0, 2_000, 10_000, 30_000)
    }
}
