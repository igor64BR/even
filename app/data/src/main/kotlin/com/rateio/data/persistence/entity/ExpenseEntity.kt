package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de uma despesa. Esqueleto mínimo (T7) — quem pagou e quanto; a
 * divisão entre participantes é escopo de uma task futura (ver `Expense` em `:domain`).
 *
 * [amountCents] guarda o valor em centavos (Long), nunca decimal cru na coluna — mesma
 * convenção do `Dinheiro` do motor de simplificação em Rateio.Domain (backend).
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
