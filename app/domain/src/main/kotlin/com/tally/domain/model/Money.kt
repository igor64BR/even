package com.tally.domain.model

/**
 * Monetary amount used in the Android domain: a wrapped integer of cents (`Long`), never raw —
 * mirrors `Money` (`backend/src/Tally.Domain/Money.cs`): inside the domain there's no
 * `Float`/`Double`/`BigDecimal`, only integer cent arithmetic — bit-for-bit reproducible with the
 * C# counterpart, a prerequisite for the Kotlin simplification engine to match the C# one on the
 * same input. `Float`/`Double`/string formatting only appear at the UI boundary (see
 * `com.tally.app.ui.format.MoneyFormat`), never here.
 *
 * `value class` is the Object Calisthenics wrap ("wrap all primitives") with no runtime allocation
 * cost — the Kotlin equivalent of the `readonly record struct` `Money.cs` uses in C#: a private
 * constructor + named factories (`ofCents`, `ZERO`) instead of exposing a raw `Long` in the public
 * constructor.
 */
@JvmInline
value class Money private constructor(val cents: Long) : Comparable<Money> {

    val isPositive: Boolean get() = cents > 0
    val isNegative: Boolean get() = cents < 0
    val isZero: Boolean get() = cents == 0L

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun unaryMinus(): Money = Money(-cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    override fun toString(): String = cents.toString()

    companion object {
        val ZERO: Money = Money(0)

        fun ofCents(cents: Long): Money = Money(cents)
    }
}
