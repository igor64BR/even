package com.rateio.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpenseTest {

    @Test
    fun `splits eh vazia por padrao, para nao quebrar quem cria Expense sem se importar com divisao`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAt = Instant.EPOCH,
        )

        assertTrue(expense.splits.isEmpty())
    }

    @Test
    fun `splits guarda a divisao por participante da despesa`() {
        val splits = listOf(
            ExpenseSplit.Equal(participantId = "p1"),
            ExpenseSplit.Equal(participantId = "p2"),
        )

        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAt = Instant.EPOCH,
            splits = splits,
        )

        assertEquals(splits, expense.splits)
    }
}
