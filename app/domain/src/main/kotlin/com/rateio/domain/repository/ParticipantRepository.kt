package com.rateio.domain.repository

import com.rateio.domain.model.Participant
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting a group's participants. `:domain` declares it, `:data` implements it
 * with Room (Dependency Inversion).
 */
interface ParticipantRepository {
    fun getParticipantsFlow(groupId: String): Flow<List<Participant>>
    suspend fun insertParticipant(participant: Participant)
    suspend fun deleteParticipant(participantId: String)
}
