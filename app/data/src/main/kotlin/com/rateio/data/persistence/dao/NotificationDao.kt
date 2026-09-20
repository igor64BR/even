package com.rateio.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rateio.data.persistence.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY occurredAtEpochMillis DESC")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    /**
     * IGNORE (não REPLACE, diferente de [com.rateio.data.persistence.dao.SettlementDao.insert]):
     * o mesmo evento pode chegar duas vezes com o mesmo [NotificationEntity.id] (tempo real +
     * pull de reconexão, T40.2) — a segunda gravação não pode reverter `isRead` de volta pra
     * `false` caso o usuário já tenha aberto a tela de notificações entre as duas.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("SELECT MAX(occurredAtEpochMillis) FROM notifications")
    suspend fun getLastEventEpochMillis(): Long?
}
