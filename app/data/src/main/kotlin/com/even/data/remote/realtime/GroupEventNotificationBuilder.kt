package com.even.data.remote.realtime

import com.even.domain.format.MoneyFormatter
import com.even.domain.model.GroupNotification
import com.even.domain.model.Participant
import java.time.Instant

/**
 * Builds the human-readable notification text, faithful to `prototype/notifications.html`:
 * "Alice logged 'Description' — $X — in 'Group name'." /
 * "Alice settled $X with Bob in 'Group name'." — built from an already-resolved
 * [GroupRealtimeEvent] (participant names, group name). Pure — no Room/SignalR/network — hence
 * testable in isolation (`GroupEventNotificationBuilderTest`).
 *
 * [build] uses the server event id prefixed by its type as [GroupNotification.id] — see the KDoc
 * of `GroupNotification.id` for the dedupe rationale.
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
        is GroupRealtimeEvent.ExpenseCreated -> "expense:$expenseId"
        is GroupRealtimeEvent.DebtSettled -> "settlement:$settlementId"
    }

    private fun GroupRealtimeEvent.messageFor(groupName: String, participants: List<Participant>): String =
        when (this) {
            is GroupRealtimeEvent.ExpenseCreated -> {
                val payerName = participants.displayNameFor(payerId, capitalized = true)
                "$payerName logged \"$description\" — ${moneyFormatter.format(amountTotalCents)} — in \"$groupName\"."
            }

            is GroupRealtimeEvent.DebtSettled -> {
                val fromName = participants.displayNameFor(fromParticipantId, capitalized = true)
                val toName = participants.displayNameFor(toParticipantId, capitalized = false)
                "$fromName settled ${moneyFormatter.format(amountCents)} with $toName in \"$groupName\"."
            }
        }
}

/** "You"/"you" for the local participant (`Participant.isYou`, same convention as the prototype); their own name for everyone else. */
private fun List<Participant>.displayNameFor(participantId: String, capitalized: Boolean): String {
    val participant = firstOrNull { it.id == participantId }
        ?: return if (capitalized) "Someone" else "someone"
    if (!participant.isYou) return participant.name
    return if (capitalized) "You" else "you"
}
