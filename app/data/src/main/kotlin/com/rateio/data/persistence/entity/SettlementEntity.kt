package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de uma quitação (`Settlement` em `:domain`, T42.1) — quem pagou,
 * quem recebeu, quanto, em qual grupo. Schema novo (v4, ver [com.rateio.data.persistence.RateioDatabase]):
 * antes de T42.1 este tipo só existia em `:domain`, sem DAO/entidade Room.
 *
 * [amountCents] guarda o valor em centavos (Long), mesma convenção de [ExpenseEntity.amountCents]
 * — nunca decimal cru na coluna.
 */
@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["payerId"],
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiverId"],
        ),
    ],
    indices = [Index("groupId"), Index("payerId"), Index("receiverId")],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val payerId: String,
    val receiverId: String,
    val amountCents: Long,
    val createdAtEpochMillis: Long,
)
