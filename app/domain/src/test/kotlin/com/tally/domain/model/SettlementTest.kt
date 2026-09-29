package com.tally.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Mirrors `case-01-simple` from `algorithm-spec.md` (A owes, B receives) in the type's shape. */
class SettlementTest {

    @Test
    fun `Settlement stores payer, receiver and amount`() {
        val settlement = Settlement(
            id = "s1",
            groupId = "g1",
            payerId = "A",
            receiverId = "B",
            amount = Money.ofCents(1000),
            createdAt = Instant.EPOCH,
        )

        assertEquals("A", settlement.payerId)
        assertEquals("B", settlement.receiverId)
        assertEquals(1000L, settlement.amount.cents)
    }
}
