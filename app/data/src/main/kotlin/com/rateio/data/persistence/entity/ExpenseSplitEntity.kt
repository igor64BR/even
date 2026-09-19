package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Mapeamento de persistência de uma parte de despesa (`ExpenseSplit` em `:domain`). Uma linha por
 * participante por despesa — [type] guarda qual subtipo de `ExpenseSplit` a linha representa;
 * [weight] só é preenchido para [SplitTypeEntity.WEIGHT] e [fixedAmountCents] só para
 * [SplitTypeEntity.FIXED_AMOUNT] (as demais colunas ficam `null`). Room não modela hierarquia
 * polimórfica de `sealed class` diretamente numa tabela — achatar pra um schema com colunas
 * opcionais por subtipo, reconstruindo o subtipo certo no mapper (`toDomain()` em
 * `RoomExpenseRepository`), é o approach padrão pra isso.
 *
 * Chave primária composta (`expenseId`, `participantId`): um participante tem no máximo uma
 * participação por despesa.
 */
@Entity(
    tableName = "expense_splits",
    primaryKeys = ["expenseId", "participantId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
        ),
    ],
    indices = [Index("expenseId"), Index("participantId")],
)
data class ExpenseSplitEntity(
    val expenseId: String,
    val participantId: String,
    val type: SplitTypeEntity,
    val weight: Long? = null,
    val fixedAmountCents: Long? = null,
)

/**
 * Espelha o subtipo concreto de `ExpenseSplit` (`:domain`) numa coluna. Room persiste enums pelo
 * nome (`TEXT`) nativamente, sem `TypeConverter` extra.
 */
enum class SplitTypeEntity {
    EQUAL,
    WEIGHT,
    FIXED_AMOUNT,
}
