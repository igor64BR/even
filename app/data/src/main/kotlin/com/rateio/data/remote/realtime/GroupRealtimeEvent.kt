package com.rateio.data.remote.realtime

import com.rateio.data.remote.groups.GrupoEventoDto
import com.rateio.data.remote.groups.TIPO_EVENTO_DESPESA_CRIADA
import com.rateio.data.remote.groups.TIPO_EVENTO_DIVIDA_QUITADA

/**
 * Representação interna de um evento de grupo, independente de transporte — o mesmo tipo serve
 * pro payload recebido ao vivo pelo SignalR ([DespesaCriadaPayload]/[DividaQuitadaPayload]) e pro
 * DTO do fallback de pull T39 ([GrupoEventoDto]), pra [GroupEventRecorder]/
 * [GroupEventNotificationBuilder] não precisarem saber de onde o evento veio.
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

internal fun DespesaCriadaPayload.toDomainEvent() = GroupRealtimeEvent.ExpenseCreated(
    expenseId = despesaId,
    description = descricao,
    amountTotalCents = valorTotalCentavos,
    payerId = pagadorId,
)

internal fun DividaQuitadaPayload.toDomainEvent() = GroupRealtimeEvent.DebtSettled(
    settlementId = quitacaoId,
    fromParticipantId = deParticipanteId,
    toParticipantId = paraParticipanteId,
    amountCents = valorCentavos,
)

/** `null` pra um `tipo` desconhecido ou um evento sem o id que o identifica (payload malformado). */
internal fun GrupoEventoDto.toDomainEvent(): GroupRealtimeEvent? = when (tipo) {
    TIPO_EVENTO_DESPESA_CRIADA -> despesaId?.let { id ->
        GroupRealtimeEvent.ExpenseCreated(
            expenseId = id,
            description = descricao.orEmpty(),
            amountTotalCents = valorTotalCentavos ?: 0L,
            payerId = pagadorId.orEmpty(),
        )
    }

    TIPO_EVENTO_DIVIDA_QUITADA -> quitacaoId?.let { id ->
        GroupRealtimeEvent.DebtSettled(
            settlementId = id,
            fromParticipantId = deParticipanteId.orEmpty(),
            toParticipantId = paraParticipanteId.orEmpty(),
            amountCents = valorCentavos ?: 0L,
        )
    }

    else -> null
}
