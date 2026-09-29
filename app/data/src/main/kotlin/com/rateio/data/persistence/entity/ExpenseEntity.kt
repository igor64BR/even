package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence mapping for an expense. Minimal skeleton (T7) — who paid and how much; the split
 * among participants is a future task's scope (see `Expense` in `:domain`).
 *
 * [amountCents] holds the amount in cents (Long), never raw decimal in the column — the same
 * convention as `Money` in the simplification engine in Rateio.Domain (backend).
 */
@Entity(
    tableName = "expenses",
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
            childColumns = ["paidByParticipantId"],
        ),
    ],
    indices = [Index("groupId"), Index("paidByParticipantId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val description: String,
    val amountCents: Long,
    val paidByParticipantId: String,
    val createdAtEpochMillis: Long,
)
