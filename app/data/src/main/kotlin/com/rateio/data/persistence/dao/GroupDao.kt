package com.rateio.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rateio.data.persistence.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAtEpochMillis DESC")
    fun getGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    /**
     * `@Upsert` (SQLite `INSERT ... ON CONFLICT DO UPDATE`) em vez de `@Insert(REPLACE)`: REPLACE
     * faz DELETE+INSERT sob o capô, o que disparava `ON DELETE CASCADE` das FKs de
     * participantes/despesas ao re-inserir um grupo já existente (ex.: marcar `isSynced=true`),
     * apagando esses dados. `@Upsert` faz UPDATE de verdade, sem DELETE, então a cascata nunca
     * dispara.
     */
    @Upsert
    suspend fun insert(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)
}
