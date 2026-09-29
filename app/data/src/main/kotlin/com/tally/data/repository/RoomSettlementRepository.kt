package com.tally.data.repository

import com.tally.data.persistence.dao.SettlementDao
import com.tally.data.persistence.entity.SettlementEntity
import com.tally.domain.model.Money
import com.tally.domain.model.Settlement
import com.tally.domain.repository.SettlementRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of [SettlementRepository] on top of [SettlementDao] (Room). */
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
