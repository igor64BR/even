package com.tally.domain.realtime

/**
 * A realtime connection to a synced group's events (T40.1/T40.2, RF35/RF36; constitution.md
 * principle 3 — our own system via a Hub, no third-party push, documented kill-state limitation).
 * `:domain` only sees "connect to a group" / "disconnect" — SignalR is a `:data` detail (the only
 * real implementation, `com.tally.data.remote.realtime.SignalRGroupRealtimeGateway`), for the
 * same reason [com.tally.domain.repository.AuthRepository]/
 * [com.tally.domain.repository.GroupRepository] don't let Retrofit/Room leak in here (Dependency
 * Inversion).
 *
 * The caller is responsible for only connecting while the synced group's screen is in focus
 * (`GroupDetailScreen`, T42) — this interface doesn't enforce that on its own, so as not to couple
 * to a specific UI lifecycle.
 */
interface GroupRealtimeGateway {

    /**
     * [localGroupId] is the group's local (Room) id — used by the implementation to resolve the
     * group/participant names when building the notification. [remoteGroupId] is the group's id on
     * the backend (`Group.remoteId`, T19.2) — what the Hub (`TallyHub.JoinGroupAsync`) and the
     * pull fallback (T39, `GET /groups/{id}/events`) expect. The two are never the same value;
     * calling with a group that doesn't have a [remoteGroupId] yet (not synced) doesn't make sense
     * — the caller guarantees that beforehand (see `GroupDetailViewModel.startRealtimeUpdates`).
     */
    suspend fun connect(localGroupId: String, remoteGroupId: String)

    /** Idempotent: calling with no active connection is not an error. */
    suspend fun disconnect()
}
