package com.tally.app.ui.createexpense

import com.tally.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers T24.2: [calculateEqualSplit] matches the exact total amount even when the split isn't
 * even (`algorithm-spec.md`, "Closing splits that don't divide evenly"). Plain JUnit — no
 * Robolectric, because the function doesn't touch Android/Room.
 */
class EqualSplitCalculatorTest {

    @Test
    fun `R$10,00 split among 3 participants matches the exact total`() {
        val parts = calculateEqualSplit(Money.ofCents(1000), listOf("p2", "p1", "p3"))

        assertEquals(1000L, parts.values.sumOf { it.cents })
        // "p1" is first in participantId order (algorithm-spec.md): gets the extra cent.
        assertEquals(334L, parts.getValue("p1").cents)
        assertEquals(333L, parts.getValue("p2").cents)
        assertEquals(333L, parts.getValue("p3").cents)
    }

    @Test
    fun `an even split between 2 participants has no cent left over or missing`() {
        val parts = calculateEqualSplit(Money.ofCents(2000), listOf("a", "b"))

        assertEquals(1000L, parts.getValue("a").cents)
        assertEquals(1000L, parts.getValue("b").cents)
    }

    @Test
    fun `no participants returns an empty map`() {
        val parts = calculateEqualSplit(Money.ofCents(1000), emptyList())

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `a remainder greater than 1 distributes an extra cent to the first participants in id order`() {
        // 1000 / 7 = base 142, remainder 6 -> 6 of the 7 participants get 143, 1 gets 142.
        val ids = listOf("g7", "g1", "g5", "g2", "g6", "g4", "g3")
        val parts = calculateEqualSplit(Money.ofCents(1000), ids)

        assertEquals(1000L, parts.values.sumOf { it.cents })
        val sortedIds = ids.sorted()
        sortedIds.take(6).forEach { id -> assertEquals("participant $id should have 143", 143L, parts.getValue(id).cents) }
        assertEquals(142L, parts.getValue(sortedIds.last()).cents)
    }
}
