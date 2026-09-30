package com.tally.data.remote.realtime

import com.tally.domain.model.Participant
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Confirms the human-readable text built by [GroupEventNotificationBuilder] matches
 * `prototype/notifications.html` ("Alice logged 'Description' — $X — in 'Group name'."/"Alice
 * settled $X with Bob in 'Group name'."), including the substitution with "You"/"you" for the
 * local participant, and the dedupe id ("expense:"/"settlement:" + the server id). Pure — no
 * Room/SignalR — [com.tally.domain.format.MoneyFormatter] is a simple test double.
 */
class GroupEventNotificationBuilderTest {

    private val builder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" })
    private val occurredAt: Instant = Instant.parse("2026-09-10T18:22:00Z")

    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "You", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    @Test
    fun `expense created by another participant builds text with their name`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e1",
            description = "Weekly groceries",
            amountTotalCents = 15_000,
            payerId = "p2",
        )

        val notification = builder.build(event, "g1", "Beach trip", participants, occurredAt)

        assertEquals("expense:e1", notification.id)
        assertEquals("g1", notification.groupId)
        assertEquals(
            "Duda logged \"Weekly groceries\" — R$ 150,00 — in \"Beach trip\".",
            notification.message,
        )
        assertEquals(occurredAt, notification.occurredAt)
    }

    @Test
    fun `expense created by the device owner uses capitalized You`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e2",
            description = "Gas",
            amountTotalCents = 5_000,
            payerId = "p1",
        )

        val notification = builder.build(event, "g1", "Beach trip", participants, occurredAt)

        assertEquals(
            "You logged \"Gas\" — R$ 50,00 — in \"Beach trip\".",
            notification.message,
        )
    }

    @Test
    fun `debt settled with you as the payee uses lowercase you`() {
        val event = GroupRealtimeEvent.DebtSettled(
            settlementId = "s1",
            fromParticipantId = "p2",
            toParticipantId = "p1",
            amountCents = 10_000,
        )

        val notification = builder.build(event, "g1", "Beach trip", participants, occurredAt)

        assertEquals("settlement:s1", notification.id)
        assertEquals(
            "Duda settled R$ 100,00 with you in \"Beach trip\".",
            notification.message,
        )
    }

    @Test
    fun `unknown participant falls back to Someone without breaking`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e3",
            description = "Ice cream",
            amountTotalCents = 1_000,
            payerId = "id-that-does-not-exist",
        )

        val notification = builder.build(event, "g1", "Beach trip", participants, occurredAt)

        assertEquals(
            "Someone logged \"Ice cream\" — R$ 10,00 — in \"Beach trip\".",
            notification.message,
        )
    }
}
