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
     * `@Upsert` (SQLite `INSERT ... ON CONFLICT DO UPDATE`) instead of `@Insert(REPLACE)`: REPLACE
     * does a DELETE+INSERT under the hood, which triggered the `ON DELETE CASCADE` of the
     * participant/expense FKs when re-inserting an already-existing group (e.g. marking
     * `isSynced=true`), wiping that data out. `@Upsert` does a real UPDATE, no DELETE, so the
     * cascade never fires.
     */
    @Upsert
    suspend fun insert(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)
}
