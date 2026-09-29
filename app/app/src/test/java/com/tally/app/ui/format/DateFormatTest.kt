package com.tally.app.ui.format

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers T41.1: [formatInstantAsRelative] reproduces the same scale as `fmtRelative()` in
 * `prototype/app.js` ("today"/"yesterday"/"N days ago"/"N months ago"), used in the notifications
 * screen.
 */
class DateFormatTest {

    private val now: Instant = Instant.parse("2026-09-20T12:00:00Z")

    @Test
    fun `an instant on the same day is today`() {
        val instant = Instant.parse("2026-09-20T08:00:00Z")

        assertEquals("today", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `an instant 1 day before is yesterday`() {
        val instant = now.minusSeconds(36 * 3600) // > 24h, < 48h

        assertEquals("yesterday", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `an instant 5 days before uses N days ago`() {
        val instant = now.minusSeconds(5L * 86_400)

        assertEquals("5 days ago", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `an instant 1 month before uses singular month`() {
        val instant = now.minusSeconds(35L * 86_400)

        assertEquals("1 month ago", formatInstantAsRelative(instant, now))
    }

    @Test
    fun `an instant 2 months before uses plural months`() {
        val instant = now.minusSeconds(65L * 86_400)

        assertEquals("2 months ago", formatInstantAsRelative(instant, now))
    }
}
