package com.rateio.app.ui.createexpense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre T26.1: soma de percentuais que não fecha 100% bloqueia [isPercentageSplitComplete] (e,
 * por extensão, `onSaveClick` em [CreateExpenseViewModel]). JUnit puro, sem Robolectric — mesma
 * convenção de [EqualSplitCalculatorTest].
 */
class PercentageSplitCalculatorTest {

    private fun row(id: String, percentageInput: String) = ExpenseSplitRowUiModel(
        participantId = id,
        name = id,
        isYou = false,
        isIncluded = true,
        percentageInput = percentageInput,
    )

    @Test
    fun `parsePercentageInput aceita inteiro nao negativo`() {
        assertEquals(30L, parsePercentageInput("30"))
        assertEquals(0L, parsePercentageInput("0"))
    }

    @Test
    fun `parsePercentageInput rejeita vazio, negativo e nao numerico`() {
        assertNull(parsePercentageInput(""))
        assertNull(parsePercentageInput("  "))
        assertNull(parsePercentageInput("-5"))
        assertNull(parsePercentageInput("abc"))
    }

    @Test
    fun `defaultPercentage arredonda 100 dividido pelo numero de participantes`() {
        assertEquals(33L, defaultPercentage(3))
        assertEquals(50L, defaultPercentage(2))
        assertEquals(0L, defaultPercentage(0))
    }

    @Test
    fun `soma 100 por cento fecha a divisao`() {
        val rows = listOf(row("p1", "40"), row("p2", "35"), row("p3", "25"))

        assertEquals(100L, sumPercentages(rows))
        assertTrue(isPercentageSplitComplete(rows))
    }

    @Test
    fun `soma diferente de 100 por cento bloqueia a divisao`() {
        val rows = listOf(row("p1", "40"), row("p2", "35"), row("p3", "20"))

        assertEquals(95L, sumPercentages(rows))
        assertFalse(isPercentageSplitComplete(rows))
    }

    @Test
    fun `entrada vazia ou invalida conta como zero na soma`() {
        val rows = listOf(row("p1", "100"), row("p2", ""), row("p3", "abc"))

        assertEquals(100L, sumPercentages(rows))
        assertTrue(isPercentageSplitComplete(rows))
    }
}
