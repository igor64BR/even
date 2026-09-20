package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre T26.2: soma de valores fixos que não fecha o total bloqueia
 * [isFixedAmountSplitComplete] (e, por extensão, `onSaveClick` em [CreateExpenseViewModel]).
 * JUnit puro, sem Robolectric — mesma convenção de [EqualSplitCalculatorTest].
 */
class FixedAmountSplitCalculatorTest {

    private fun row(id: String, fixedAmountInput: String) = ExpenseSplitRowUiModel(
        participantId = id,
        name = id,
        isYou = false,
        isIncluded = true,
        fixedAmountInput = fixedAmountInput,
    )

    @Test
    fun `parseFixedAmountInput aceita virgula e ponto como separador decimal`() {
        assertEquals(1050L, parseFixedAmountInput("10,50")?.cents)
        assertEquals(1050L, parseFixedAmountInput("10.50")?.cents)
    }

    @Test
    fun `parseFixedAmountInput rejeita vazio e negativo`() {
        assertNull(parseFixedAmountInput(""))
        assertNull(parseFixedAmountInput("-1,00"))
    }

    @Test
    fun `soma que fecha o valor total permite salvar`() {
        val total = Money.ofCents(2000)
        val rows = listOf(row("p1", "12,00"), row("p2", "8,00"))

        assertEquals(2000L, sumFixedAmounts(rows).cents)
        assertTrue(isFixedAmountSplitComplete(rows, total))
    }

    @Test
    fun `soma diferente do valor total bloqueia salvar`() {
        val total = Money.ofCents(2000)
        val rows = listOf(row("p1", "12,00"), row("p2", "7,00"))

        assertEquals(1900L, sumFixedAmounts(rows).cents)
        assertFalse(isFixedAmountSplitComplete(rows, total))
    }

    @Test
    fun `entrada vazia ou invalida conta como zero na soma`() {
        val total = Money.ofCents(1000)
        val rows = listOf(row("p1", "10,00"), row("p2", ""), row("p3", "abc"))

        assertEquals(1000L, sumFixedAmounts(rows).cents)
        assertTrue(isFixedAmountSplitComplete(rows, total))
    }
}
