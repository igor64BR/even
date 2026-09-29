package com.tally.domain.engine

import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Money
import com.tally.domain.model.Settlement
import com.tally.domain.model.SettlementSuggestion
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * One test per case in the "Test cases" table in
 * `specs/001-mvp-expense-splitting/algorithm-spec.md`, named with each case's stable id (e.g.
 * `case-01-simple` -> [case01Simple]). The five `case-*` cases are the minimum floor required by
 * the spec and by T33; the extra tests at the end of the file (sum invariant, weighted split,
 * fixed-amount split) mirror the same extra tests in `DebtSimplificationEngineTests.cs` (T31), to
 * prove the two ports of the engine haven't diverged in any split mode.
 *
 * The numeric values here need to match, number for number, with
 * `backend/tests/Tally.Domain.Tests/DebtSimplificationEngineTests.cs`.
 */
class GreedyDebtSimplificationEngineTest {

    private val engine: DebtSimplificationEngine = GreedyDebtSimplificationEngine()

    @Test
    fun `case-01-simple a debt between two people`() {
        val balances = mapOf(
            "A" to Money.ofCents(-1000),
            "B" to Money.ofCents(1000),
        )

        val transactions = engine.computeSettlement(balances)

        assertEquals(listOf(SettlementSuggestion("A", "B", Money.ofCents(1000))), transactions)
    }

    @Test
    fun `case-02-cycle pairwise debts cancel out in the net balance`() {
        val balances = mapOf(
            "A" to Money.ZERO,
            "B" to Money.ZERO,
            "C" to Money.ZERO,
        )

        val transactions = engine.computeSettlement(balances)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `case-03-zeroed group already settled generates no transaction`() {
        val balances = mapOf(
            "A" to Money.ZERO,
            "B" to Money.ZERO,
            "C" to Money.ZERO,
        )

        val transactions = engine.computeSettlement(balances)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `case-04-long-chain greedy resolves five participants in three transactions`() {
        val balances = mapOf(
            "A" to Money.ofCents(-4000),
            "B" to Money.ofCents(-3000),
            "C" to Money.ofCents(1000),
            "D" to Money.ofCents(2000),
            "E" to Money.ofCents(4000),
        )

        val transactions = engine.computeSettlement(balances)

        val expected = listOf(
            SettlementSuggestion("A", "E", Money.ofCents(4000)),
            SettlementSuggestion("B", "D", Money.ofCents(2000)),
            SettlementSuggestion("B", "C", Money.ofCents(1000)),
        )
        assertEquals(expected, transactions)
    }

    @Test
    fun `case-05-rounding largest-remainder closing does not lose a cent`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Lunch",
            amountCents = 1000,
            paidByParticipantId = "P1",
            createdAt = Instant.EPOCH,
            splits = listOf(
                ExpenseSplit.Equal(participantId = "P1"),
                ExpenseSplit.Equal(participantId = "P2"),
                ExpenseSplit.Equal(participantId = "P3"),
            ),
        )

        val balances = engine.computeBalances(listOf(expense), settlements = emptyList())

        assertEquals(Money.ofCents(666), balances.getValue("P1"))
        assertEquals(Money.ofCents(-333), balances.getValue("P2"))
        assertEquals(Money.ofCents(-333), balances.getValue("P3"))
        assertEquals(0L, sumCents(balances))

        val transactions = engine.computeSettlement(balances)

        val expected = listOf(
            SettlementSuggestion("P2", "P1", Money.ofCents(333)),
            SettlementSuggestion("P3", "P1", Money.ofCents(333)),
        )
        assertEquals(expected, transactions)
    }

    @Test
    fun `computeBalances sum of balances is always zero even with an expense and a settlement`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Lunch",
            amountCents = 1000,
            paidByParticipantId = "P1",
            createdAt = Instant.EPOCH,
            splits = listOf(
                ExpenseSplit.Equal(participantId = "P1"),
                ExpenseSplit.Equal(participantId = "P2"),
                ExpenseSplit.Equal(participantId = "P3"),
            ),
        )
        val settlement = Settlement(id = "s1", groupId = "g1", payerId = "P2", receiverId = "P1", amount = Money.ofCents(100), createdAt = Instant.EPOCH)

        val balances = engine.computeBalances(listOf(expense), listOf(settlement))

        assertEquals(0L, sumCents(balances))
    }

    @Test
    fun `computeBalances weighted split uses the largest-remainder method`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Rent",
            amountCents = 100,
            paidByParticipantId = "X",
            createdAt = Instant.EPOCH,
            splits = listOf(
                ExpenseSplit.Weight(participantId = "X", weight = 1),
                ExpenseSplit.Weight(participantId = "Y", weight = 1),
                ExpenseSplit.Weight(participantId = "Z", weight = 1),
            ),
        )

        val balances = engine.computeBalances(listOf(expense), settlements = emptyList())

        // 100 / 3 = base 33, remainder 1 -> X (first in id order) gets the extra cent: 34/33/33.
        assertEquals(Money.ofCents(66), balances.getValue("X"))
        assertEquals(Money.ofCents(-33), balances.getValue("Y"))
        assertEquals(Money.ofCents(-33), balances.getValue("Z"))
        assertEquals(0L, sumCents(balances))
    }

    @Test
    fun `computeBalances fixed-amount split uses each participant's amount without rounding`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Hotel",
            amountCents = 1000,
            paidByParticipantId = "X",
            createdAt = Instant.EPOCH,
            splits = listOf(
                ExpenseSplit.FixedAmount(participantId = "X", amount = Money.ofCents(400)),
                ExpenseSplit.FixedAmount(participantId = "Y", amount = Money.ofCents(600)),
            ),
        )

        val balances = engine.computeBalances(listOf(expense), settlements = emptyList())

        assertEquals(Money.ofCents(600), balances.getValue("X"))
        assertEquals(Money.ofCents(-600), balances.getValue("Y"))
        assertEquals(0L, sumCents(balances))
    }

    private fun sumCents(balances: Map<String, Money>): Long = balances.values.sumOf { it.cents }
}
