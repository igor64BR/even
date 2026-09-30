package com.tally.data.remote.realtime

import com.tally.domain.model.Group
import com.tally.domain.model.GroupNotification
import com.tally.domain.model.Participant
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.NotificationRepository
import com.tally.domain.repository.ParticipantRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers "event received via Hub records a local notification" — at the level of the
 * collaborator that actually does this, [GroupEventRecorder], with no SignalR/Room at all (simple
 * test doubles for [GroupRepository]/[ParticipantRepository]/[NotificationRepository], same pattern
 * as `RemoteGroupSyncRepositoryTest` in `:data`).
 */
class GroupEventRecorderTest {

    private val group = Group(id = "g1", name = "Beach trip", createdAt = Instant.EPOCH)
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "You", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    @Test
    fun `record saves a local notification from the event`() = runTest {
        val notificationRepository = FakeNotificationRepository()
        val recorder = GroupEventRecorder(
            notificationRepository = notificationRepository,
            groupRepository = FakeGroupRepository(group),
            participantRepository = FakeParticipantRepository(participants),
            notificationBuilder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" }),
        )
        val event = GroupRealtimeEvent.ExpenseCreated(
            expenseId = "e1",
            description = "Groceries",
            amountTotalCents = 3_000,
            payerId = "p2",
        )

        recorder.record("g1", event)

        val saved = notificationRepository.inserted.single()
        assertEquals("expense:e1", saved.id)
        assertEquals("g1", saved.groupId)
        assertTrue(saved.message.contains("Duda logged \"Groceries\""))
    }

    @Test
    fun `record saves nothing if the local group no longer exists`() = runTest {
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

        recorder.record("deleted-group", event)

        assertTrue("a deleted local group must not generate an orphan notification", notificationRepository.inserted.isEmpty())
    }

    private class FakeGroupRepository(private val group: Group?) : GroupRepository {
        override fun getGroupsFlow(): Flow<List<Group>> = throw UnsupportedOperationException("not used in this test")
        override suspend fun getGroupById(groupId: String): Group? = group
        override suspend fun insertGroup(group: Group) = throw UnsupportedOperationException("not used in this test")
        override suspend fun deleteGroup(groupId: String) = throw UnsupportedOperationException("not used in this test")
    }

    private class FakeParticipantRepository(private val participants: List<Participant>) : ParticipantRepository {
        override fun getParticipantsFlow(groupId: String): Flow<List<Participant>> = MutableStateFlow(participants)
        override suspend fun insertParticipant(participant: Participant) =
            throw UnsupportedOperationException("not used in this test")

        override suspend fun deleteParticipant(participantId: String) =
            throw UnsupportedOperationException("not used in this test")
    }

    private class FakeNotificationRepository : NotificationRepository {
        val inserted = mutableListOf<GroupNotification>()

        override fun getNotificationsFlow(): Flow<List<GroupNotification>> = MutableStateFlow(inserted)
        override fun getUnreadCountFlow(): Flow<Int> = MutableStateFlow(inserted.count { !it.isRead })
        override suspend fun insert(notification: GroupNotification) {
            inserted += notification
        }

        override suspend fun markAllAsRead() = throw UnsupportedOperationException("not used in this test")
        override suspend fun getLastEventTimestamp(): Instant? = inserted.maxOfOrNull { it.occurredAt }
    }
}
