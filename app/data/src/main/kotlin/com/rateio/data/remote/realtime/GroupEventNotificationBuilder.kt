package com.rateio.data.remote.realtime

import com.rateio.domain.format.MoneyFormatter
import com.rateio.domain.model.GroupNotification
import com.rateio.domain.model.Participant
import java.time.Instant

/**
 * Monta o texto humano da notificação, fiel a `prototype/notificacoes.html`:
 * "Fulano lançou 'Descrição' — R$X — em 'Nome do grupo'." /
 * "Fulano quitou R$X com Beltrano em 'Nome do grupo'." — a partir de um [GroupRealtimeEvent] já
 * resolvido (nomes de participantes, nome do grupo). Pura — sem Room/SignalR/rede — por isso
 * testável isoladamente (`GroupEventNotificationBuilderTest`).
 *
 * [build] usa o id do evento no servidor prefixado pelo tipo como [GroupNotification.id] — ver
 * KDoc de `GroupNotification.id` pro racional de dedupe.
 */
internal class GroupEventNotificationBuilder(private val moneyFormatter: MoneyFormatter) {

    fun build(
        event: GroupRealtimeEvent,
        localGroupId: String,
        groupName: String,
        participants: List<Participant>,
        occurredAt: Instant,
    ): GroupNotification = GroupNotification(
        id = event.notificationId(),
        groupId = localGroupId,
        message = event.messageFor(groupName, participants),
        occurredAt = occurredAt,
    )

    private fun GroupRealtimeEvent.notificationId(): String = when (this) {
        is GroupRealtimeEvent.ExpenseCreated -> "despesa:$expenseId"
        is GroupRealtimeEvent.DebtSettled -> "quitacao:$settlementId"
    }

    private fun GroupRealtimeEvent.messageFor(groupName: String, participants: List<Participant>): String =
        when (this) {
            is GroupRealtimeEvent.ExpenseCreated -> {
                val payerName = participants.displayNameFor(payerId, capitalized = true)
                "$payerName lançou \"$description\" — ${moneyFormatter.format(amountTotalCents)} — em \"$groupName\"."
            }

            is GroupRealtimeEvent.DebtSettled -> {
                val fromName = participants.displayNameFor(fromParticipantId, capitalized = true)
                val toName = participants.displayNameFor(toParticipantId, capitalized = false)
                "$fromName quitou ${moneyFormatter.format(amountCents)} com $toName em \"$groupName\"."
            }
        }
}

/** "Você"/"você" pro participante local (`Participant.isYou`, mesma convenção do protótipo); nome próprio pros demais. */
private fun List<Participant>.displayNameFor(participantId: String, capitalized: Boolean): String {
    val participant = firstOrNull { it.id == participantId }
        ?: return if (capitalized) "Alguém" else "alguém"
    if (!participant.isYou) return participant.name
    return if (capitalized) "Você" else "você"
}
