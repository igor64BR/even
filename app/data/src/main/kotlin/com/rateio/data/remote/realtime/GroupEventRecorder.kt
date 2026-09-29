package com.rateio.data.remote.realtime

import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * "A group event happened, record the local notification" — resolves the local group/participants
 * (Room) and records via [NotificationRepository]. Used both by [SignalRGroupRealtimeGateway]
 * (live event) and by [MissedGroupEventsSynchronizer] (T39, reconnect pull), to avoid duplicating
 * this logic in both places — extracted precisely to be testable without any SignalR
 * (`GroupEventRecorderTest`, with test doubles for [GroupRepository]/[ParticipantRepository]/
 * [NotificationRepository]).
 *
 * Silently records nothing if [localGroupId] no longer exists locally (group deleted between the
 * event firing and reaching here) — not an error in the notification flow.
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
