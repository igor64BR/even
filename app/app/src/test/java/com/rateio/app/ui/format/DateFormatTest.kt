package com.rateio.app.ui.format

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cobre T41.1: [formatInstantAsRelative] reproduz a mesma escala de `fmtRelative()` em
 * `prototype/app.js` ("hoje"/"ontem"/"há N dias"/"há N meses"), usada na tela de notificações.
 */
class DateFormatTest {

    private val now: Instant = Instant.parse("2026-09-20T12:00:00Z")

    @Test
    fun `instante no mesmo dia e hoje`() {
        val instant = Instant.parse("2026-09-20T08:00:00Z")

        assertEquals("hoje", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `instante 1 dia antes e ontem`() {
        val instant = now.minusSeconds(36 * 3600) // > 24h, < 48h

        assertEquals("ontem", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `instante 5 dias antes usa ha N dias`() {
        val instant = now.minusSeconds(5L * 86_400)

        assertEquals("há 5 dias", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `instante 1 mes antes usa singular mes`() {
        val instant = now.minusSeconds(35L * 86_400)

        assertEquals("há 1 mês", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `instante 2 meses antes usa plural meses`() {
        val instant = now.minusSeconds(65L * 86_400)

        assertEquals("há 2 meses", formatInstantAsRelative(instant, now))
    }
}
