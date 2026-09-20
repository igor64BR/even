package com.rateio.domain.engine

import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Money
import com.rateio.domain.model.Settlement
import com.rateio.domain.model.SettlementSuggestion
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Um teste por caso da tabela "Casos de teste" em `specs/001-mvp-expense-splitting/algorithm-spec.md`,
 * nomeado com o id estável de cada caso (ex. `case-01-simples` -> [case01Simples]). Os cinco
 * casos `case-*` são o piso mínimo exigido pela spec e por T33; os testes extras no fim do
 * arquivo (invariante da soma, divisão por peso, divisão por valor fixo) espelham os mesmos
 * testes extras de `MotorDeSimplificacaoDeDividasTests.cs` (T31), para provar que as duas portas
 * do motor não divergiram em nenhum modo de divisão.
 *
 * Os valores numéricos aqui precisam bater, número por número, com
 * `backend/tests/Rateio.Domain.Tests/MotorDeSimplificacaoDeDividasTests.cs`.
 */
class GreedyDebtSimplificationEngineTest {

    private val engine: DebtSimplificationEngine = GreedyDebtSimplificationEngine()

    @Test
    fun `case-01-simples uma divida entre duas pessoas`() {
        val balances = mapOf(
            "A" to Money.ofCents(-1000),
            "B" to Money.ofCents(1000),
        )

        val transactions = engine.computeSettlement(balances)

        assertEquals(listOf(SettlementSuggestion("A", "B", Money.ofCents(1000))), transactions)
    }

    @Test
    fun `case-02-ciclo dividas par-a-par se cancelam no saldo liquido`() {
        val balances = mapOf(
            "A" to Money.ZERO,
            "B" to Money.ZERO,
            "C" to Money.ZERO,
        )

        val transactions = engine.computeSettlement(balances)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `case-03-zerado grupo ja quitado nao gera transacao`() {
        val balances = mapOf(
            "A" to Money.ZERO,
            "B" to Money.ZERO,
            "C" to Money.ZERO,
        )

        val transactions = engine.computeSettlement(balances)

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `case-04-cadeia-longa guloso resolve cinco participantes em tres transacoes`() {
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
    fun `case-05-arredondamento fechamento por maiores restos nao perde centavo`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Almoço",
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
    fun `computeBalances soma dos saldos eh sempre zero mesmo com despesa e quitacao`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Almoço",
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
    fun `computeBalances divisao por peso usa metodo dos maiores restos`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Aluguel",
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

        // 100 / 3 = base 33, resto 1 -> X (primeiro em ordem de id) recebe o centavo extra: 34/33/33.
        assertEquals(Money.ofCents(66), balances.getValue("X"))
        assertEquals(Money.ofCents(-33), balances.getValue("Y"))
        assertEquals(Money.ofCents(-33), balances.getValue("Z"))
        assertEquals(0L, sumCents(balances))
    }

    @Test
    fun `computeBalances divisao por valor fixo usa o valor de cada participante sem arredondar`() {
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
