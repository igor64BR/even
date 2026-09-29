package com.rateio.domain.repository

import com.rateio.domain.model.Group
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting groups. `:domain` declares it, `:data` implements it with Room
 * (Dependency Inversion) — no Room type leaks into this interface.
 */
interface GroupRepository {
    fun getGroupsFlow(): Flow<List<Group>>
    suspend fun getGroupById(groupId: String): Group?
    suspend fun insertGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
}
