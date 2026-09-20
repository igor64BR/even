package com.rateio.data.remote.realtime

import com.rateio.domain.model.Group
import com.rateio.domain.model.GroupNotification
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre T40.1: "evento recebido via Hub grava notificação local" — no nível do colaborador que de
 * fato faz isso, [GroupEventRecorder], sem SignalR/Room nenhum (dublês simples de
 * [GroupRepository]/[ParticipantRepository]/[NotificationRepository], mesmo padrão de
 * `RemoteGroupSyncRepositoryTest` em `:data`).
 */
class GroupEventRecorderTest {

    private val group = Group(id = "g1", name = "Viagem pra praia", createdAt = Instant.EPOCH)
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "Você", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    @Test
    fun `record grava uma notificacao local a partir do evento`() = runTest {
        val notificationRepository = FakeNotificationRepository()
        val recorder = GroupEventRecorder(
            notificationRepository = notificationRepository,
            groupRepository = FakeGroupRepository(group),
            participantRepository = FakeParticipantRepository(participants),
            notificationBuilder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" }),
        )
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e1",
            description = "Mercado",
            amountTotalCents = 3_000,
            payerId = "p2",
        )

        recorder.record("g1", event)

        val saved = notificationRepository.inserted.single()
        assertEquals("despesa:e1", saved.id)
        assertEquals("g1", saved.groupId)
        assertTrue(saved.message.contains("Duda lançou \"Mercado\""))
    }

    @Test
    fun `record nao grava nada se o grupo local nao existe mais`() = runTest {
        val notificationRepository = FakeNotificationRepository()
        val recorder = GroupEventRecorder(
            notificationRepository = notificationRepository,
            groupRepository = FakeGroupRepository(group = null),
            participantRepository = FakeParticipantRepository(participants),
            notificationBuilder = GroupEventNotificationBuilder(moneyFormatter = { "" }),
        )
        val event = GroupRealtimeEvent.DebtSettled(
            settlementId = "s1",
            fromParticipantId = "p1",
            toParticipantId = "p2",
            amountCents = 1_000,
        )

        recorder.record("grupo-apagado", event)

        assertTrue("grupo local apagado nao pode gerar notificacao orfa", notificationRepository.inserted.isEmpty())
    }

    private class FakeGroupRepository(private val group: Group?) : GroupRepository {
        override fun getGroupsFlow(): Flow<List<Group>> = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun getGroupById(groupId: String): Group? = group
        override suspend fun insertGroup(group: Group) = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun deleteGroup(groupId: String) = throw UnsupportedOperationException("não usado neste teste")
    }

    private class FakeParticipantRepository(private val participants: List<Participant>) : ParticipantRepository {
        override fun getParticipantsFlow(groupId: String): Flow<List<Participant>> = MutableStateFlow(participants)
        override suspend fun insertParticipant(participant: Participant) =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun deleteParticipant(participantId: String) =
            throw UnsupportedOperationException("não usado neste teste")
    }

    private class FakeNotificationRepository : NotificationRepository {
        val inserted = mutableListOf<GroupNotification>()

        override fun getNotificationsFlow(): Flow<List<GroupNotification>> = MutableStateFlow(inserted)
        override fun getUnreadCountFlow(): Flow<Int> = MutableStateFlow(inserted.count { !it.isRead })
        override suspend fun insert(notification: GroupNotification) {
            inserted += notification
        }

        override suspend fun markAllAsRead() = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun getLastEventTimestamp(): Instant? = inserted.maxOfOrNull { it.occurredAt }
    }
}
