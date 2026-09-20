package com.rateio.data.repository

import com.rateio.data.persistence.dao.NotificationDao
import com.rateio.data.persistence.entity.NotificationEntity
import com.rateio.domain.model.GroupNotification
import com.rateio.domain.repository.NotificationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementação de [NotificationRepository] sobre [NotificationDao] (Room), T40.1. */
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
