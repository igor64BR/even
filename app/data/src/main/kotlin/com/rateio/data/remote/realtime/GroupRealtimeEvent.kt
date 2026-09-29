package com.rateio.data.remote.realtime

import com.rateio.data.remote.groups.EVENT_TYPE_DEBT_SETTLED
import com.rateio.data.remote.groups.EVENT_TYPE_EXPENSE_CREATED
import com.rateio.data.remote.groups.GroupEventDto

/**
 * Transport-independent internal representation of a group event — the same type serves both the
 * payload received live over SignalR ([ExpenseCreatedPayload]/[DebtSettledPayload]) and the T39
 * pull-fallback DTO ([GroupEventDto]), so [GroupEventRecorder]/[GroupEventNotificationBuilder]
 * don't need to know where the event came from.
 */
internal sealed interface GroupRealtimeEvent {
    data class ExpenseCreated(
        val expenseId: String,
        val description: String,
        val amountTotalCents: Long,
        val payerId: String,
    ) : GroupRealtimeEvent

    data class DebtSettled(
        val settlementId: String,
        val fromParticipantId: String,
        val toParticipantId: String,
        val amountCents: Long,
    ) : GroupRealtimeEvent
}

internal fun ExpenseCreatedPayload.toDomainEvent() = GroupRealtimeEvent.ExpenseCreated(
    expenseId = expenseId,
    description = description,
    amountTotalCents = totalAmountCents,
    payerId = payerId,
)

internal fun DebtSettledPayload.toDomainEvent() = GroupRealtimeEvent.DebtSettled(
    settlementId = settlementId,
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    amountCents = amountCents,
)

/** `null` for an unknown [type] or an event missing the id that identifies it (malformed payload). */
internal fun GroupEventDto.toDomainEvent(): GroupRealtimeEvent? = when (type) {
    EVENT_TYPE_EXPENSE_CREATED -> expenseId?.let { id ->
        GroupRealtimeEvent.ExpenseCreated(
            expenseId = id,
            description = description.orEmpty(),
            amountTotalCents = totalAmountCents ?: 0L,
            payerId = payerId.orEmpty(),
        )
    }

    EVENT_TYPE_DEBT_SETTLED -> settlementId?.let { id ->
        GroupRealtimeEvent.DebtSettled(
            settlementId = id,
            fromParticipantId = fromParticipantId.orEmpty(),
            toParticipantId = toParticipantId.orEmpty(),
            amountCents = amountCents ?: 0L,
        )
    }

    else -> null
}
