package com.tally.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the same money convention required of both ports of the engine (C#/`Money.cs`,
 * Kotlin/`Money`): everything in integer cents, no `Float`/`Double`.
 */
class MoneyTest {

    @Test
    fun `ZERO has zero cents and is zero`() {
        assertEquals(0L, Money.ZERO.cents)
        assertTrue(Money.ZERO.isZero)
        assertFalse(Money.ZERO.isPositive)
        assertFalse(Money.ZERO.isNegative)
    }

    @Test
    fun `ofCents stores the exact amount in cents`() {
        val tenReais = Money.ofCents(1000)

        assertEquals(1000L, tenReais.cents)
        assertTrue(tenReais.isPositive)
    }

    @Test
    fun `ofCents accepts a negative value and marks isNegative`() {
        val debt = Money.ofCents(-333)

        assertTrue(debt.isNegative)
        assertFalse(debt.isPositive)
    }

    @Test
    fun `addition and subtraction operate on cents`() {
        val total = Money.ofCents(1000)
        val part = Money.ofCents(334)

        assertEquals(1334L, (total + part).cents)
        assertEquals(666L, (total - part).cents)
    }

    @Test
    fun `unary minus flips the sign`() {
        val owed = Money.ofCents(500)

        assertEquals(Money.ofCents(-500), -owed)
    }

    @Test
    fun `compareTo orders by cents`() {
        val small = Money.ofCents(100)
        val big = Money.ofCents(200)

        assertTrue(small < big)
        assertTrue(big > small)
        assertEquals(0, Money.ofCents(50).compareTo(Money.ofCents(50)))
    }
}
