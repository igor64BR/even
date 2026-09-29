package com.tally.data.repository

import com.tally.data.persistence.dao.GroupDao
import com.tally.data.persistence.entity.GroupEntity
import com.tally.domain.model.Group
import com.tally.domain.repository.GroupRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of [GroupRepository] on top of [GroupDao] (Room). */
class RoomGroupRepository(private val groupDao: GroupDao) : GroupRepository {

    override fun getGroupsFlow(): Flow<List<Group>> =
        groupDao.getGroupsFlow().map { entities -> entities.map(GroupEntity::toDomain) }

    override suspend fun getGroupById(groupId: String): Group? =
        groupDao.getGroupById(groupId)?.toDomain()

    override suspend fun insertGroup(group: Group) =
        groupDao.insert(group.toEntity())

    override suspend fun deleteGroup(groupId: String) =
        groupDao.deleteById(groupId)
}

private fun GroupEntity.toDomain() = Group(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    isSynced = isSynced,
    remoteId = remoteId,
)

private fun Group.toEntity() = GroupEntity(
    id = id,
    name = name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    isSynced = isSynced,
    remoteId = remoteId,
)
