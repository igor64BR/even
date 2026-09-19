package com.rateio.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cobre a mesma convenção de dinheiro que `algorithm-spec.md` exige das duas portas do motor
 * (C#/`Dinheiro.cs`, Kotlin/`Money`): tudo em centavos inteiros, sem `Float`/`Double`.
 */
class MoneyTest {

    @Test
    fun `ZERO tem zero centavos e eh zero`() {
        assertEquals(0L, Money.ZERO.cents)
        assertTrue(Money.ZERO.isZero)
        assertFalse(Money.ZERO.isPositive)
        assertFalse(Money.ZERO.isNegative)
    }

    @Test
    fun `ofCents guarda o valor exato em centavos`() {
        val tenReais = Money.ofCents(1000)

        assertEquals(1000L, tenReais.cents)
        assertTrue(tenReais.isPositive)
    }

    @Test
    fun `ofCents aceita negativo e marca isNegative`() {
        val debt = Money.ofCents(-333)

        assertTrue(debt.isNegative)
        assertFalse(debt.isPositive)
    }

    @Test
    fun `soma e subtracao operam em centavos`() {
        val total = Money.ofCents(1000)
        val part = Money.ofCents(334)

        assertEquals(1334L, (total + part).cents)
        assertEquals(666L, (total - part).cents)
    }

    @Test
    fun `unario negativo inverte o sinal`() {
        val owed = Money.ofCents(500)

        assertEquals(Money.ofCents(-500), -owed)
    }

    @Test
    fun `compareTo ordena por centavos`() {
        val small = Money.ofCents(100)
        val big = Money.ofCents(200)

        assertTrue(small < big)
        assertTrue(big > small)
        assertEquals(0, Money.ofCents(50).compareTo(Money.ofCents(50)))
    }
}
