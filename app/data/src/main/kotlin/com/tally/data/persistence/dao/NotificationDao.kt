package com.tally.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.data.persistence.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY occurredAtEpochMillis DESC")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    /**
     * IGNORE (not REPLACE, unlike [com.tally.data.persistence.dao.SettlementDao.insert]): the
     * same event can arrive twice with the same [NotificationEntity.id] (live + reconnect pull)
     * — the second write must not revert `isRead` back to `false` if the user already
     * opened the notifications screen between the two.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("SELECT MAX(occurredAtEpochMillis) FROM notifications")
    suspend fun getLastEventEpochMillis(): Long?
}
