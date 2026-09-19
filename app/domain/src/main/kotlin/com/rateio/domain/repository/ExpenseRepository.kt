package com.rateio.domain.repository

import com.rateio.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistência de despesas de um grupo. `:domain` declara, `:data` implementa com
 * Room (Dependency Inversion).
 */
interface ExpenseRepository {
    fun getExpensesFlow(groupId: String): Flow<List<Expense>>
    suspend fun insertExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}
