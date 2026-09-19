package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre T24.2: [calculateEqualSplit] fecha o valor total exato mesmo quando a divisão não é
 * inteira (`algorithm-spec.md`, "Fechando divisões que não dão exato"). JUnit puro — sem
 * Robolectric, porque a função não toca Android/Room.
 */
class EqualSplitCalculatorTest {

    @Test
    fun `R$10,00 dividido entre 3 participantes fecha o total exato`() {
        val parts = calculateEqualSplit(Money.ofCents(1000), listOf("p2", "p1", "p3"))

        assertEquals(1000L, parts.values.sumOf { it.cents })
        // "p1" é o primeiro na ordem de participantId (algorithm-spec.md): recebe o centavo extra.
        assertEquals(334L, parts.getValue("p1").cents)
        assertEquals(333L, parts.getValue("p2").cents)
        assertEquals(333L, parts.getValue("p3").cents)
    }

    @Test
    fun `divisao exata entre 2 participantes nao sobra nem falta centavo`() {
        val parts = calculateEqualSplit(Money.ofCents(2000), listOf("a", "b"))

        assertEquals(1000L, parts.getValue("a").cents)
        assertEquals(1000L, parts.getValue("b").cents)
    }

    @Test
    fun `sem participantes retorna mapa vazio`() {
        val parts = calculateEqualSplit(Money.ofCents(1000), emptyList())

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `resto maior que 1 distribui um centavo extra pros primeiros participantes em ordem de id`() {
        // 1000 / 7 = base 142, resto 6 -> 6 dos 7 participantes recebem 143, 1 recebe 142.
        val ids = listOf("g7", "g1", "g5", "g2", "g6", "g4", "g3")
        val parts = calculateEqualSplit(Money.ofCents(1000), ids)

        assertEquals(1000L, parts.values.sumOf { it.cents })
        val sortedIds = ids.sorted()
        sortedIds.take(6).forEach { id -> assertEquals("participante $id deveria ter 143", 143L, parts.getValue(id).cents) }
        assertEquals(142L, parts.getValue(sortedIds.last()).cents)
    }
}
