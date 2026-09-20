package com.rateio.data.repository

import com.rateio.data.persistence.dao.SettlementDao
import com.rateio.data.persistence.entity.SettlementEntity
import com.rateio.domain.model.Money
import com.rateio.domain.model.Settlement
import com.rateio.domain.repository.SettlementRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementação de [SettlementRepository] sobre [SettlementDao] (Room). */
class RoomSettlementRepository(private val settlementDao: SettlementDao) : SettlementRepository {

    override fun getSettlementsFlow(groupId: String): Flow<List<Settlement>> =
        settlementDao.getSettlementsFlow(groupId).map { entities -> entities.map(SettlementEntity::toDomain) }

    override suspend fun insertSettlement(settlement: Settlement) =
        settlementDao.insert(settlement.toEntity())
}

private fun SettlementEntity.toDomain() = Settlement(
    id = id,
    groupId = groupId,
    payerId = payerId,
    receiverId = receiverId,
    amount = Money.ofCents(amountCents),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

private fun Settlement.toEntity() = SettlementEntity(
    id = id,
    groupId = groupId,
    payerId = payerId,
    receiverId = receiverId,
    amountCents = amount.cents,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)
