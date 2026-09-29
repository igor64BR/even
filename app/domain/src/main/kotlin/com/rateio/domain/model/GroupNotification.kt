package com.rateio.domain.model

import java.time.Instant

/**
 * A local notification for a synced group event (T40/T41, RF35/RF36; constitution.md principle 3
 * — our own system via a Hub, no third-party push) — an expense logged or a debt settled by
 * someone, received live (SignalR) or recovered by the pull fallback (T39).
 *
 * [message] already comes ready to display (faithful to `prototype/notifications.html`: "Alice
 * logged 'Description' — $X — in 'Group name'.") — only
 * `com.rateio.data.remote.realtime.GroupEventNotificationBuilder` builds that text; `:domain`
 * doesn't need to know how.
 *
 * [id] isn't a random UUID: it's the event's id on the server (`expenseId`/`settlementId`)
 * prefixed by its type — a natural key that automatically deduplicates the same event arriving
 * twice (once live via SignalR, once via T39's reconnect pull), with no extra "already seen this
 * event" logic needed (see `NotificationDao.insert`, `OnConflictStrategy.IGNORE`).
 */
data class GroupNotification(
    val id: String,
    val groupId: String,
    val message: String,
    val occurredAt: Instant,
    val isRead: Boolean = false,
)
