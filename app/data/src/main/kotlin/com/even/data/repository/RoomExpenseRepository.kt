package com.even.data.repository

import com.even.data.persistence.dao.ExpenseDao
import com.even.data.persistence.entity.ExpenseEntity
import com.even.data.persistence.entity.ExpenseSplitEntity
import com.even.data.persistence.entity.ExpenseWithSplitsEntity
import com.even.data.persistence.entity.SplitTypeEntity
import com.even.domain.model.Expense
import com.even.domain.model.ExpenseSplit
import com.even.domain.model.Money
import com.even.domain.repository.ExpenseRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of [ExpenseRepository] on top of [ExpenseDao] (Room). */
class RoomExpenseRepository(private val expenseDao: ExpenseDao) : ExpenseRepository {

    override fun getExpensesFlow(groupId: String): Flow<List<Expense>> =
        expenseDao.getExpensesWithSplitsFlow(groupId)
            .map { rows -> rows.map(ExpenseWithSplitsEntity::toDomain) }

    override suspend fun getExpenseById(expenseId: String): Expense? =
        expenseDao.getExpenseWithSplitsById(expenseId)?.toDomain()

    override suspend fun insertExpense(expense: Expense) =
        expenseDao.insertWithSplits(
            expense = expense.toEntity(),
            splits = expense.splits.map { split -> split.toEntity(expenseId = expense.id) },
        )

    override suspend fun deleteExpense(expenseId: String) =
        expenseDao.deleteById(expenseId)
}

private fun ExpenseWithSplitsEntity.toDomain() = Expense(
    id = expense.id,
    groupId = expense.groupId,
    description = expense.description,
    amountCents = expense.amountCents,
    paidByParticipantId = expense.paidByParticipantId,
    createdAt = Instant.ofEpochMilli(expense.createdAtEpochMillis),
    splits = splits.map(ExpenseSplitEntity::toDomain),
)

private fun Expense.toEntity() = ExpenseEntity(
    id = id,
    groupId = groupId,
    description = description,
    amountCents = amountCents,
    paidByParticipantId = paidByParticipantId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun ExpenseSplit.toEntity(expenseId: String): ExpenseSplitEntity = when (this) {
    is ExpenseSplit.Equal -> ExpenseSplitEntity(
        expenseId = expenseId,
        participantId = participantId,
        type = SplitTypeEntity.EQUAL,
    )

    is ExpenseSplit.Weight -> ExpenseSplitEntity(
        expenseId = expenseId,
        participantId = participantId,
        type = SplitTypeEntity.WEIGHT,
        weight = weight,
    )

    is ExpenseSplit.FixedAmount -> ExpenseSplitEntity(
        expenseId = expenseId,
        participantId = participantId,
        type = SplitTypeEntity.FIXED_AMOUNT,
        fixedAmountCents = amount.cents,
    )
}

private fun ExpenseSplitEntity.toDomain(): ExpenseSplit = when (type) {
    SplitTypeEntity.EQUAL -> ExpenseSplit.Equal(participantId = participantId)

    SplitTypeEntity.WEIGHT -> ExpenseSplit.Weight(
        participantId = participantId,
        weight = requireNotNull(weight) {
            "ExpenseSplitEntity of type WEIGHT with no weight (participantId=$participantId)"
        },
    )

    SplitTypeEntity.FIXED_AMOUNT -> ExpenseSplit.FixedAmount(
        participantId = participantId,
        amount = Money.ofCents(
            requireNotNull(fixedAmountCents) {
                "ExpenseSplitEntity of type FIXED_AMOUNT with no fixedAmountCents (participantId=$participantId)"
            },
        ),
    )
}
