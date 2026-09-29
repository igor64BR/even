package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers T26.2: a fixed-amount sum that doesn't match the total blocks
 * [isFixedAmountSplitComplete] (and, by extension, `onSaveClick` in [CreateExpenseViewModel]).
 * Plain JUnit, no Robolectric — same convention as [EqualSplitCalculatorTest].
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
    fun `parseFixedAmountInput accepts comma and dot as the decimal separator`() {
        assertEquals(1050L, parseFixedAmountInput("10,50")?.cents)
        assertEquals(1050L, parseFixedAmountInput("10.50")?.cents)
    }

    @Test
    fun `parseFixedAmountInput rejects empty and negative input`() {
        assertNull(parseFixedAmountInput(""))
        assertNull(parseFixedAmountInput("-1,00"))
    }

    @Test
    fun `a sum matching the total amount allows saving`() {
        val total = Money.ofCents(2000)
        val rows = listOf(row("p1", "12,00"), row("p2", "8,00"))

        assertEquals(2000L, sumFixedAmounts(rows).cents)
        assertTrue(isFixedAmountSplitComplete(rows, total))
    }

    @Test
    fun `a sum different from the total amount blocks saving`() {
        val total = Money.ofCents(2000)
        val rows = listOf(row("p1", "12,00"), row("p2", "7,00"))

        assertEquals(1900L, sumFixedAmounts(rows).cents)
        assertFalse(isFixedAmountSplitComplete(rows, total))
    }

    @Test
    fun `an empty or invalid input counts as zero in the sum`() {
        val total = Money.ofCents(1000)
        val rows = listOf(row("p1", "10,00"), row("p2", ""), row("p3", "abc"))

        assertEquals(1000L, sumFixedAmounts(rows).cents)
        assertTrue(isFixedAmountSplitComplete(rows, total))
    }
}
