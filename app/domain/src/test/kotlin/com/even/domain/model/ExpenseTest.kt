package com.even.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpenseTest {

    @Test
    fun `splits is empty by default, so it does not break code that creates an Expense without caring about the split`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAt = Instant.EPOCH,
        )

        assertTrue(expense.splits.isEmpty())
    }

    @Test
    fun `splits stores the expense's per-participant split`() {
        val splits = listOf(
            ExpenseSplit.Equal(participantId = "p1"),
            ExpenseSplit.Equal(participantId = "p2"),
        )

        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAt = Instant.EPOCH,
            splits = splits,
        )

        assertEquals(splits, expense.splits)
    }
}
