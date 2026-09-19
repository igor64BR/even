package com.rateio.domain.model

/**
 * Valor monetário usado no domínio Android: um inteiro de centavos (`Long`) wrapped, nunca cru —
 * espelha `Dinheiro` (`backend/src/Rateio.Domain/Dinheiro.cs`) e a decisão registrada em
 * "Dinheiro: representação e arredondamento" em `algorithm-spec.md`: dentro do domínio não existe
 * `Float`/`Double`/`BigDecimal`, só aritmética inteira de centavos — reproduzível bit a bit com a
 * contraparte C#, pré-requisito pra T33 (motor de simplificação em Kotlin) bater com T31 (C#) na
 * mesma entrada. `Float`/`Double`/formatação de string só aparecem na borda de UI (ver
 * `com.rateio.app.ui.format.MoneyFormat`), nunca aqui.
 *
 * `value class` é o wrap de Object Calisthenics ("wrap all primitives") sem custo de alocação em
 * runtime — equivalente Kotlin do `readonly record struct` que `Dinheiro.cs` usa em C#: construtor
 * privado + fábricas nomeadas (`ofCents`, `ZERO`) em vez de expor `Long` cru no construtor público.
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
