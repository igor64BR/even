package com.rateio.domain.repository

import com.rateio.domain.model.Participant
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistência de participantes de um grupo. `:domain` declara, `:data` implementa
 * com Room (Dependency Inversion).
 */
interface ParticipantRepository {
    fun getParticipantsFlow(groupId: String): Flow<List<Participant>>
    suspend fun insertParticipant(participant: Participant)
    suspend fun deleteParticipant(participantId: String)
}
