package com.rateio.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Espelha `case-01-simples` de `algorithm-spec.md` (A deve, B recebe) no shape do tipo. */
class SettlementTest {

    @Test
    fun `Settlement guarda pagador, recebedor e valor`() {
        val settlement = Settlement(
            id = "s1",
            groupId = "g1",
            payerId = "A",
            receiverId = "B",
            amount = Money.ofCents(1000),
        )

        assertEquals("A", settlement.payerId)
        assertEquals("B", settlement.receiverId)
        assertEquals(1000L, settlement.amount.cents)
    }
}
