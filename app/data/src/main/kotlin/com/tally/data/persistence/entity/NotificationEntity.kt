package com.tally.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence mapping for `com.tally.domain.model.GroupNotification` (T40.1/T41.1). [id] is the
 * event's id on the server prefixed by its type ("expense:"/"settlement:", see
 * `com.tally.data.remote.realtime.GroupEventNotificationBuilder`) — a natural key that
 * automatically deduplicates the same event arriving twice (live + reconnect pull, T40.2) via
 * `OnConflictStrategy.IGNORE` instead of a random UUID.
 *
 * [occurredAtEpochMillis] is the device's local time at the moment the event was processed, not a
 * server timestamp — neither the live payload (`ExpenseCreatedEvent`/`DebtSettledEvent`, backend
 * T38) nor the pull-fallback response (T39) carry a timestamp of when the event actually happened
 * (gap documented in `com.tally.data.remote.groups.GroupEventsApi`). Schema v5 (T40.1) — no
 * explicit migration, same rationale as earlier versions (`TallyDatabase`): there's still no
 * distributed build.
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId"), Index("occurredAtEpochMillis")],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val message: String,
    val occurredAtEpochMillis: Long,
    val isRead: Boolean,
)
