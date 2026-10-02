package com.even.data.repository

import com.even.data.persistence.dao.NotificationDao
import com.even.data.persistence.entity.NotificationEntity
import com.even.domain.model.GroupNotification
import com.even.domain.repository.NotificationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of [NotificationRepository] on top of [NotificationDao] (Room). */
class RoomNotificationRepository(private val notificationDao: NotificationDao) : NotificationRepository {

    override fun getNotificationsFlow(): Flow<List<GroupNotification>> =
        notificationDao.getNotificationsFlow().map { entities -> entities.map(NotificationEntity::toDomain) }

    override fun getUnreadCountFlow(): Flow<Int> = notificationDao.getUnreadCountFlow()

    override suspend fun insert(notification: GroupNotification) = notificationDao.insert(notification.toEntity())

    override suspend fun markAllAsRead() = notificationDao.markAllAsRead()

    override suspend fun getLastEventTimestamp(): Instant? =
        notificationDao.getLastEventEpochMillis()?.let(Instant::ofEpochMilli)
}

private fun NotificationEntity.toDomain() = GroupNotification(
    id = id,
    groupId = groupId,
    message = message,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    isRead = isRead,
)

private fun GroupNotification.toEntity() = NotificationEntity(
    id = id,
    groupId = groupId,
    message = message,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    isRead = isRead,
)
