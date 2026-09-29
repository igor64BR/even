package com.tally.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * One case per [ExpenseSplit] subtype (Equal/Weight/FixedAmount — RF17-RF19), mirroring the
 * fields of `ExpenseSplit.cs`. The actual cent distribution (who gets the remainder,
 * `case-05-rounding` in `algorithm-spec.md`) is the simplification engine's job — T33, out of
 * scope for T7B; here we only guarantee the type carries the right data per subtype.
 */
class ExpenseSplitTest {

    @Test
    fun `Equal only stores participantId`() {
        val split = ExpenseSplit.Equal(participantId = "p1")

        assertEquals("p1", split.participantId)
    }

    @Test
    fun `Weight stores participantId and weight`() {
        val split = ExpenseSplit.Weight(participantId = "p2", weight = 2)

        assertEquals("p2", split.participantId)
        assertEquals(2L, split.weight)
    }

    @Test
    fun `FixedAmount stores participantId and a Money`() {
        val split = ExpenseSplit.FixedAmount(participantId = "p3", amount = Money.ofCents(1500))

        assertEquals("p3", split.participantId)
        assertEquals(Money.ofCents(1500), split.amount)
    }

    @Test
    fun `different subtypes with the same participantId are not equal`() {
        // ExpenseSplit doesn't carry a loose type enum (see the class KDoc) -- equality has to
        // come from the sealed class's concrete subtype, otherwise an Equal("p1") and a
        // Weight("p1", 0) could be confused.
        val equal: ExpenseSplit = ExpenseSplit.Equal(participantId = "p1")
        val weight: ExpenseSplit = ExpenseSplit.Weight(participantId = "p1", weight = 0)

        assertNotEquals(equal, weight)
    }

    @Test
    fun `equal split among 3 participants mirrors the shape of case-05-rounding`() {
        // Same scenario as algorithm-spec.md (a 1000-cent expense among P1,P2,P3): here we only
        // model the list of splits the engine (T33) will consume, without computing who gets the
        // extra cent -- that's computeBalances/splitEqually, not this type.
        val splits = listOf(
            ExpenseSplit.Equal(participantId = "P1"),
            ExpenseSplit.Equal(participantId = "P2"),
            ExpenseSplit.Equal(participantId = "P3"),
        )

        assertEquals(listOf("P1", "P2", "P3"), splits.map { it.participantId })
    }
}
