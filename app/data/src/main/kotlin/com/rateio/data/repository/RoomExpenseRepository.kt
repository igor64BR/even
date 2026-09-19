package com.rateio.data.repository

import com.rateio.data.persistence.dao.ExpenseDao
import com.rateio.data.persistence.entity.ExpenseEntity
import com.rateio.domain.model.Expense
import com.rateio.domain.repository.ExpenseRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementação de [ExpenseRepository] sobre [ExpenseDao] (Room). */
class RoomExpenseRepository(private val expenseDao: ExpenseDao) : ExpenseRepository {

    override fun getExpensesFlow(groupId: String): Flow<List<Expense>> =
        expenseDao.getExpensesFlow(groupId).map { entities -> entities.map(ExpenseEntity::toDomain) }

    override suspend fun insertExpense(expense: Expense) =
        expenseDao.insert(expense.toEntity())

    override suspend fun deleteExpense(expenseId: String) =
        expenseDao.deleteById(expenseId)
}

private fun ExpenseEntity.toDomain() = Expense(
    id = id,
    groupId = groupId,
    description = description,
    amountCents = amountCents,
    paidByParticipantId = paidByParticipantId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

private fun Expense.toEntity() = ExpenseEntity(
    id = id,
    groupId = groupId,
    description = description,
    amountCents = amountCents,
    paidByParticipantId = paidByParticipantId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)
