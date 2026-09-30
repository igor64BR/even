package com.tally.data.remote.realtime

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.groups.GroupEventsApi
import com.tally.domain.repository.NotificationRepository
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import retrofit2.HttpException

/**
 * Pull fallback (no third-party push): fetches
 * events missed since the last known local notification and records each one via
 * [GroupEventRecorder]. [SignalRGroupRealtimeGateway] calls [sync] both on the first successful
 * connection and on every reconnection — covering "network dropped and came back" as well
 * as "app was killed and reopened" (without this second case, an event missed while the app was
 * fully closed would never show up: there's no reconnection to trigger the pull in that scenario —
 * an accepted kill-state limitation trade-off).
 *
 * Extracted from [SignalRGroupRealtimeGateway] to be testable without any `HubConnection`
 * (`MissedGroupEventsSynchronizerTest`, with a test double [GroupEventsApi] — same pattern as
 * `RemoteGroupSyncRepositoryTest`): [SignalRGroupRealtimeGateway] only decides WHEN to call this,
 * this class decides WHAT to do when called.
 *
 * [since]: `GroupsController.GetEvents` (`[FromQuery] DateTimeOffset
 * since` with no default) requires the parameter to always be present, so we never omit `since` —
 * with no local notification yet ([NotificationRepository.getLastEventTimestamp] `null`), we ask
 * since [Instant.EPOCH] (equivalent to "the whole history"), never omitting the parameter (that
 * would give a 400 from ASP.NET Core's model binding before it even reaches the use case).
 *
 * Never propagates failure to the caller: a network failure can't bring down the realtime
 * connection because of this.
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
            // No connection to the backend — the next successful reconnection tries again.
        } catch (error: HttpException) {
            // Denied access (403) or the group no longer exists on the server (404) — same
            // rationale: never bring down the realtime connection because of this.
        }
    }

    private companion object {
        val ISO_INSTANT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    }
}
