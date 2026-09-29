package com.tally.app.ui.createexpense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers T26.1: a percentage sum that doesn't add up to 100% blocks [isPercentageSplitComplete]
 * (and, by extension, `onSaveClick` in [CreateExpenseViewModel]). Plain JUnit, no Robolectric —
 * same convention as [EqualSplitCalculatorTest].
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
    fun `parsePercentageInput accepts a non-negative integer`() {
        assertEquals(30L, parsePercentageInput("30"))
        assertEquals(0L, parsePercentageInput("0"))
    }

    @Test
    fun `parsePercentageInput rejects empty, negative and non-numeric input`() {
        assertNull(parsePercentageInput(""))
        assertNull(parsePercentageInput("  "))
        assertNull(parsePercentageInput("-5"))
        assertNull(parsePercentageInput("abc"))
    }

    @Test
    fun `defaultPercentage rounds 100 divided by the number of participants`() {
        assertEquals(33L, defaultPercentage(3))
        assertEquals(50L, defaultPercentage(2))
        assertEquals(0L, defaultPercentage(0))
    }

    @Test
    fun `a sum of 100 percent completes the split`() {
        val rows = listOf(row("p1", "40"), row("p2", "35"), row("p3", "25"))

        assertEquals(100L, sumPercentages(rows))
        assertTrue(isPercentageSplitComplete(rows))
    }

    @Test
    fun `a sum other than 100 percent blocks the split`() {
        val rows = listOf(row("p1", "40"), row("p2", "35"), row("p3", "20"))

        assertEquals(95L, sumPercentages(rows))
        assertFalse(isPercentageSplitComplete(rows))
    }

    @Test
    fun `an empty or invalid input counts as zero in the sum`() {
        val rows = listOf(row("p1", "100"), row("p2", ""), row("p3", "abc"))

        assertEquals(100L, sumPercentages(rows))
        assertTrue(isPercentageSplitComplete(rows))
    }
}
