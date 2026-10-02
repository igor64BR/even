package com.even.data.remote.realtime

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.even.data.local.auth.TokenStorage
import com.even.data.remote.groups.GroupEventsApi
import com.even.domain.format.MoneyFormatter
import com.even.domain.realtime.GroupRealtimeGateway
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.NotificationRepository
import com.even.domain.repository.ParticipantRepository
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
 * Real implementation of [GroupRealtimeGateway] on top of the official Java SignalR
 * client (`com.microsoft.signalr:signalr`). Connects to [hubUrl] (`/hubs/even`, see
 * `NotificationHubRoute` on the backend), authenticated via an access token in the query string
 * (`withAccessTokenProvider` — the same mechanism the backend's `OnMessageReceived` reads for Hub
 * connections, since WebSocket doesn't send an `Authorization` header in the handshake; see the
 * KDoc of `EvenHub`), joins the remote group's SignalR group (`JoinGroupAsync`) and listens for
 * `ExpenseCreated`/`DebtSettled` — the exact names `SignalRGroupEventNotifier` uses to send
 * `nameof(GroupEventType.ExpenseCreated/DebtSettled)`.
 *
 * **Reconnection is manual, not library-provided**: unlike the SignalR JS/.NET clients, the
 * official Java client doesn't expose `withAutomaticReconnect`/`onReconnected` (checked against the
 * public API of `HubConnectionBuilder`/`HubConnection` — only `onClosed` exists; it's a known gap
 * of the Java client, not an omission in this code). [maintainConnection] implements the retry
 * manually: on every `onClosed`, it waits for a backoff (same progression documented for the JS
 * client — 0s, 2s, 10s, 30s, then repeating 30s) and reconnects. Every successful connection — the
 * first one and every reconnection — triggers [MissedGroupEventsSynchronizer.sync], which
 * also covers "app was killed and reopened" in addition to "network dropped and came back" (see
 * the KDoc of [MissedGroupEventsSynchronizer]).
 *
 * **No real unit test for Hub connection**: this
 * class is the only piece of the flow that actually opens a socket, and there's no way to test it
 * without a real Hub running (Robolectric doesn't spin up an ASP.NET server, and adding a network
 * mocking framework for this would be out of scope for this task). All the business logic that
 * matters — building the notification text, deciding what to persist, fetching missed events —
 * was extracted into [GroupEventNotificationBuilder], [GroupEventRecorder] and
 * [MissedGroupEventsSynchronizer], all three tested in isolation with test doubles, no SignalR at
 * all. This class was verified manually (careful reading of the client's public API, see the
 * javadocs of `HubConnection`/`HubConnectionBuilder`) and by inspecting the artifact's published
 * POM (RxJava3 + Gson — not RxJava2 or Jackson, as an outdated reading of the docs might suggest).
 *
 * A single group at a time: [connect] always closes a previous connection before opening a new one
 * (never two simultaneous connections) — consistent with "only connects when a synced group is
 * being viewed" (not a multi-group feature).
 *
 * Builds [GroupEventNotificationBuilder]/[GroupEventRecorder]/[MissedGroupEventsSynchronizer]
 * internally from the "raw" dependencies (Room/Retrofit/[MoneyFormatter]) instead of receiving them
 * ready-made: the three are `internal` to this module (only used by this class and tested directly
 * by `:data`), and [com.even.app.di.AppContainer], in `:app`, shouldn't need to know these
 * composition details — just enough to assemble the gateway as a whole.
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
     * Single loop (no recursion) that keeps the connection alive: connects, waits for it to close
     * ([CompletableDeferred] completed by the connection's `onClosed`), reconnects with backoff.
     * Runs entirely in [scope] — cancelling [scope] (via [disconnect]) stops the loop at the next
     * suspension point.
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
                connection.invoke("JoinGroupAsync", arrayOf<Any>(remoteGroupId)).blockingAwait()
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
            EVENT_EXPENSE_CREATED,
            { payload: ExpenseCreatedPayload -> recordEvent(scope, localGroupId, payload.toDomainEvent()) },
            ExpenseCreatedPayload::class.java,
        )
        connection.on(
            EVENT_DEBT_SETTLED,
            { payload: DebtSettledPayload -> recordEvent(scope, localGroupId, payload.toDomainEvent()) },
            DebtSettledPayload::class.java,
        )
        return connection
    }

    private fun recordEvent(scope: CoroutineScope, localGroupId: String, event: GroupRealtimeEvent) {
        scope.launch { eventRecorder.record(localGroupId, event) }
    }

    private fun reconnectDelayMillisFor(attempt: Int): Long =
        RECONNECT_DELAYS_MILLIS.getOrElse(attempt - 1) { RECONNECT_DELAYS_MILLIS.last() }

    private companion object {
        const val EVENT_EXPENSE_CREATED = "ExpenseCreated"
        const val EVENT_DEBT_SETTLED = "DebtSettled"
        val RECONNECT_DELAYS_MILLIS = longArrayOf(0, 2_000, 10_000, 30_000)
    }
}
