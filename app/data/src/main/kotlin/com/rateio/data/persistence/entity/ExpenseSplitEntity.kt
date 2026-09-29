package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Persistence mapping for an expense split (`ExpenseSplit` in `:domain`). One row per participant
 * per expense — [type] holds which `ExpenseSplit` subtype the row represents; [weight] is only
 * populated for [SplitTypeEntity.WEIGHT] and [fixedAmountCents] only for
 * [SplitTypeEntity.FIXED_AMOUNT] (the other columns stay `null`). Room doesn't model a `sealed
 * class`'s polymorphic hierarchy directly in a table — flattening it into a schema with optional
 * columns per subtype, reconstructing the right subtype in the mapper (`toDomain()` in
 * `RoomExpenseRepository`), is the standard approach for this.
 *
 * Composite primary key (`expenseId`, `participantId`): a participant has at most one split per
 * expense.
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
 * Mirrors `ExpenseSplit`'s (`:domain`) concrete subtype in a column. Room persists enums by name
 * (`TEXT`) natively, with no extra `TypeConverter`.
 */
enum class SplitTypeEntity {
    EQUAL,
    WEIGHT,
    FIXED_AMOUNT,
}
