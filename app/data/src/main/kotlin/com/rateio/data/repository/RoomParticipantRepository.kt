package com.rateio.data.repository

import com.rateio.data.persistence.dao.ParticipantDao
import com.rateio.data.persistence.entity.ParticipantEntity
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.ParticipantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of [ParticipantRepository] on top of [ParticipantDao] (Room). */
class RoomParticipantRepository(private val participantDao: ParticipantDao) : ParticipantRepository {

    override fun getParticipantsFlow(groupId: String): Flow<List<Participant>> =
        participantDao.getParticipantsFlow(groupId).map { entities -> entities.map(ParticipantEntity::toDomain) }

    override suspend fun insertParticipant(participant: Participant) =
        participantDao.insert(participant.toEntity())

    override suspend fun deleteParticipant(participantId: String) =
        participantDao.deleteById(participantId)
}

private fun ParticipantEntity.toDomain() = Participant(
    id = id,
    groupId = groupId,
    name = name,
    isYou = isYou,
)

private fun Participant.toEntity() = ParticipantEntity(
    id = id,
    groupId = groupId,
    name = name,
    isYou = isYou,
)
