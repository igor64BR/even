package com.rateio.data.persistence.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Junta uma [ExpenseEntity] com suas [ExpenseSplitEntity] (uma despesa : N partes) pra Room
 * resolver a relação com `@Relation` em vez de duas queries soltas montadas manualmente — ver uso
 * em `com.rateio.data.persistence.dao.ExpenseDao.getExpensesWithSplitsFlow`.
 */
data class ExpenseWithSplitsEntity(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val splits: List<ExpenseSplitEntity>,
)
