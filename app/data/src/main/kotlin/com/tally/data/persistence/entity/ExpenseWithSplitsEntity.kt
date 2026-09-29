package com.tally.data.persistence.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Joins an [ExpenseEntity] with its [ExpenseSplitEntity]s (one expense : N splits) so Room can
 * resolve the relation with `@Relation` instead of two loose queries assembled by hand — see the
 * usage in `com.tally.data.persistence.dao.ExpenseDao.getExpensesWithSplitsFlow`.
 */
data class ExpenseWithSplitsEntity(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val splits: List<ExpenseSplitEntity>,
)
