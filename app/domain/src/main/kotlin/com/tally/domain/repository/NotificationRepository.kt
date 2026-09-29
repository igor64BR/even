package com.tally.domain.repository

import com.tally.domain.model.GroupNotification
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting local notifications (T40/T41, RF35/RF36). `:domain` declares it, `:data`
 * implements it with Room (Dependency Inversion) — same pattern as [SettlementRepository]: no Room
 * type leaks into this interface.
 */
interface NotificationRepository {
    fun getNotificationsFlow(): Flow<List<GroupNotification>>

    /** Unread count — source of the "Notifications" tab badge (T41.2, `TallyBottomBar`). */
    fun getUnreadCountFlow(): Flow<Int>

    /** Idempotent by [GroupNotification.id] — see the KDoc of [GroupNotification.id]. */
    suspend fun insert(notification: GroupNotification)

    /** The "Notifications" screen (T41.1) marks everything as read when closed — faithful to the prototype. */
    suspend fun markAllAsRead()

    /**
     * The instant of the most recent notification already persisted — used as `since` in the pull
     * fallback call (T39, see `com.tally.domain.realtime.GroupRealtimeGateway`). `null` if no
     * notification has been recorded yet (the pull fetches the whole available history).
     */
    suspend fun getLastEventTimestamp(): Instant?
}
