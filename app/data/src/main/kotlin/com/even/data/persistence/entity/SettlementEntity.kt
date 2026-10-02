package com.even.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence mapping for a settlement (`Settlement` in `:domain`) — who paid, who
 * received, how much, in which group. New schema (v4, see
 * [com.even.data.persistence.EvenDatabase]): this type previously only existed in `:domain`,
 * with no DAO/Room entity.
 *
 * [amountCents] holds the amount in cents (Long), the same convention as
 * [ExpenseEntity.amountCents] — never raw decimal in the column.
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
