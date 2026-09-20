package com.rateio.data.remote.realtime

import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * "Um evento de grupo aconteceu, grave a notificação local" — resolve grupo/participantes locais
 * (Room) e grava via [NotificationRepository]. Usado tanto por [SignalRGroupRealtimeGateway]
 * (evento ao vivo) quanto por [MissedGroupEventsSynchronizer] (T39, pull de reconexão), pra não
 * duplicar essa lógica nos dois lugares — extraída justamente pra ser testável sem SignalR nenhum
 * (`GroupEventRecorderTest`, com dublês de [GroupRepository]/[ParticipantRepository]/
 * [NotificationRepository]).
 *
 * Silenciosamente não grava nada se [localGroupId] não existir mais localmente (grupo apagado
 * entre o evento ter sido disparado e chegar aqui) — não é um erro do fluxo de notificação.
 */
internal class GroupEventRecorder(
    private val notificationRepository: NotificationRepository,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val notificationBuilder: GroupEventNotificationBuilder,
) {
    suspend fun record(localGroupId: String, event: GroupRealtimeEvent) {
        val group = groupRepository.getGroupById(localGroupId) ?: return
        val participants = participantRepository.getParticipantsFlow(localGroupId).first()
        val notification = notificationBuilder.build(
            event = event,
            localGroupId = localGroupId,
            groupName = group.name,
            participants = participants,
            occurredAt = Instant.now(),
        )
        notificationRepository.insert(notification)
    }
}
