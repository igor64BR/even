package com.tally.domain.repository

import com.tally.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting a group's expenses. `:domain` declares it, `:data` implements it with
 * Room (Dependency Inversion).
 */
interface ExpenseRepository {
    fun getExpensesFlow(groupId: String): Flow<List<Expense>>

    /** One-off read of an expense by id — used to pre-fill the edit form. `null` if it no longer exists. */
    suspend fun getExpenseById(expenseId: String): Expense?
    suspend fun insertExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}
