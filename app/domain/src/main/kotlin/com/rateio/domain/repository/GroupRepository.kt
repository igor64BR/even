package com.rateio.domain.repository

import com.rateio.domain.model.Group
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistência de grupos. `:domain` declara, `:data` implementa com Room
 * (Dependency Inversion) — nenhum tipo do Room vaza para esta interface.
 */
interface GroupRepository {
    fun getGroupsFlow(): Flow<List<Group>>
    suspend fun getGroupById(groupId: String): Group?
    suspend fun insertGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
}
