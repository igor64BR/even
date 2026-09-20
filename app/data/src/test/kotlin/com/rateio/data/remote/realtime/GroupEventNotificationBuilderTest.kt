package com.rateio.data.remote.realtime

import com.rateio.domain.model.Participant
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cobre T40/T41: o texto humano montado por [GroupEventNotificationBuilder] bate com
 * `prototype/notificacoes.html` ("Fulano lançou 'Descrição' — R$X — em 'Nome do grupo'."/"Fulano
 * quitou R$X com Beltrano em 'Nome do grupo'."), incluindo a substituição por "Você"/"você" pro
 * participante local, e o id de dedupe ("despesa:"/"quitacao:" + id do servidor). Pura — sem
 * Room/SignalR — [com.rateio.domain.format.MoneyFormatter] é um dublê simples.
 */
class GroupEventNotificationBuilderTest {

    private val builder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" })
    private val occurredAt: Instant = Instant.parse("2026-09-10T18:22:00Z")

    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "Você", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    @Test
    fun `despesa criada por outro participante monta texto com nome dele`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e1",
            description = "Mercado da semana",
            amountTotalCents = 15_000,
            payerId = "p2",
        )

        val notification = builder.build(event, "g1", "Viagem pra praia", participants, occurredAt)

        assertEquals("despesa:e1", notification.id)
        assertEquals("g1", notification.groupId)
        assertEquals(
            "Duda lançou \"Mercado da semana\" — R$ 150,00 — em \"Viagem pra praia\".",
            notification.message,
        )
        assertEquals(occurredAt, notification.occurredAt)
    }

    @Test
    fun `despesa criada pelo proprio dono do aparelho usa Você maiusculo`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e2",
            description = "Gasolina",
            amountTotalCents = 5_000,
            payerId = "p1",
        )

        val notification = builder.build(event, "g1", "Viagem pra praia", participants, occurredAt)

        assertEquals(
            "Você lançou \"Gasolina\" — R$ 50,00 — em \"Viagem pra praia\".",
            notification.message,
        )
    }

    @Test
    fun `divida quitada com voce como recebedor usa voce minusculo`() {
        val event = GroupRealtimeEvent.DebtSettled(
            settlementId = "s1",
            fromParticipantId = "p2",
            toParticipantId = "p1",
            amountCents = 10_000,
        )

        val notification = builder.build(event, "g1", "Viagem pra praia", participants, occurredAt)

        assertEquals("quitacao:s1", notification.id)
        assertEquals(
            "Duda quitou R$ 100,00 com você em \"Viagem pra praia\".",
            notification.message,
        )
    }

    @Test
    fun `participante desconhecido cai no fallback Alguem sem quebrar`() {
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e3",
            description = "Sorvete",
            amountTotalCents = 1_000,
            payerId = "id-que-nao-existe",
        )

        val notification = builder.build(event, "g1", "Viagem pra praia", participants, occurredAt)

        assertEquals(
            "Alguém lançou \"Sorvete\" — R$ 10,00 — em \"Viagem pra praia\".",
            notification.message,
        )
    }
}
