package com.rateio.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Um caso por subtipo de [ExpenseSplit] (Igual/Peso/ValorFixo — RF17-RF19), espelhando os campos
 * de `ParticipacaoDespesa.cs`. A distribuição de centavos em si (quem recebe o resto,
 * `case-05-arredondamento` de `algorithm-spec.md`) é o motor de simplificação — T33, fora do
 * escopo de T7B; aqui só se garante que o tipo carrega o dado certo por subtipo.
 */
class ExpenseSplitTest {

    @Test
    fun `Equal guarda so o participantId`() {
        val split = ExpenseSplit.Equal(participantId = "p1")

        assertEquals("p1", split.participantId)
    }

    @Test
    fun `Weight guarda participantId e peso`() {
        val split = ExpenseSplit.Weight(participantId = "p2", weight = 2)

        assertEquals("p2", split.participantId)
        assertEquals(2L, split.weight)
    }

    @Test
    fun `FixedAmount guarda participantId e um Money`() {
        val split = ExpenseSplit.FixedAmount(participantId = "p3", amount = Money.ofCents(1500))

        assertEquals("p3", split.participantId)
        assertEquals(Money.ofCents(1500), split.amount)
    }

    @Test
    fun `subtipos diferentes com mesmo participantId nao sao iguais`() {
        // ExpenseSplit nao carrega um enum de tipo solto (ver KDoc da classe) -- a igualdade tem
        // que vir do subtipo concreto do sealed class, senao um Equal("p1") e um
        // Weight("p1", 0) poderiam ser confundidos.
        val equal: ExpenseSplit = ExpenseSplit.Equal(participantId = "p1")
        val weight: ExpenseSplit = ExpenseSplit.Weight(participantId = "p1", weight = 0)

        assertNotEquals(equal, weight)
    }

    @Test
    fun `divisao igual entre 3 participantes espelha o shape de case-05-arredondamento`() {
        // Mesmo cenario de algorithm-spec.md (despesa de 1000 centavos entre P1,P2,P3): aqui so
        // se modela a lista de participacoes que o motor (T33) vai consumir, sem calcular quem
        // recebe o centavo extra -- isso e computeBalances/dividirIgualmente, nao este tipo.
        val splits = listOf(
            ExpenseSplit.Equal(participantId = "P1"),
            ExpenseSplit.Equal(participantId = "P2"),
            ExpenseSplit.Equal(participantId = "P3"),
        )

        assertEquals(listOf("P1", "P2", "P3"), splits.map { it.participantId })
    }
}
