package com.even.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.even.data.persistence.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE groupId = :groupId")
    fun getSettlementsFlow(groupId: String): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settlement: SettlementEntity)
}
