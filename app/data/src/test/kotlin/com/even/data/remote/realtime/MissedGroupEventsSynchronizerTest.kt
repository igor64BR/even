package com.even.data.remote.realtime

import com.even.data.local.auth.TokenStorage
import com.even.data.remote.groups.GroupEventDto
import com.even.data.remote.groups.GroupEventsApi
import com.even.domain.model.AuthSession
import com.even.domain.model.AuthenticatedUser
import com.even.domain.model.Group
import com.even.domain.model.GroupNotification
import com.even.domain.model.Participant
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.NotificationRepository
import com.even.domain.repository.ParticipantRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Covers "reconnection fetches missed events" — at the level of the collaborator
 * that actually calls the endpoint, [MissedGroupEventsSynchronizer], with no `HubConnection` at
 * all. [GroupEventsApi] is a simple test double (same pattern as `RemoteGroupSyncRepositoryTest`);
 * it was never exercised against a real `GET /groups/{id}/events` — gap documented in
 * `GroupEventsApi`.
 */
class MissedGroupEventsSynchronizerTest {

    private val session = AuthSession(
        accessToken = "valid-access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )
    private val group = Group(id = "g1", name = "Beach trip", createdAt = Instant.EPOCH)
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "You", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    private fun buildSynchronizer(
        groupEventsApi: GroupEventsApi,
        tokenStorage: TokenStorage,
        notificationRepository: NotificationRepository = FakeNotificationRepository(),
    ) = MissedGroupEventsSynchronizer(
        groupEventsApi = groupEventsApi,
        tokenStorage = tokenStorage,
        notificationRepository = notificationRepository,
        eventRecorder = GroupEventRecorder(
            notificationRepository = notificationRepository,
            groupRepository = FakeGroupRepository(group),
            participantRepository = FakeParticipantRepository(participants),
            notificationBuilder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" }),
        ),
    )

    @Test
    fun `sync fetches since the last known timestamp and records the returned events`() = runTest {
        val notificationRepository = FakeNotificationRepository(
            existing = mutableListOf(
                GroupNotification(id = "n0", groupId = "g1", message = "old", occurredAt = Instant.ofEpochMilli(5_000)),
            ),
        )
        val api = FakeGroupEventsApi(
            events = listOf(
                GroupEventDto(
                    type = 0,
                    groupId = "remote-g1",
                    expenseId = "e1",
                    description = "Groceries",
                    totalAmountCents = 3_000,
                    payerId = "p2",
                ),
            ),
        )
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session), notificationRepository)

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertEquals("Bearer valid-access-token", api.lastBearerToken)
        assertEquals("remote-g1", api.lastGroupId)
        assertEquals("1970-01-01T00:00:05Z", api.lastSince)
        val inserted = notificationRepository.inserted.single()
        assertEquals("expense:e1", inserted.id)
        assertTrue(inserted.message.contains("Duda logged \"Groceries\""))
    }

    @Test
    fun `sync with no prior notification sends since Instant EPOCH (backend always requires the parameter)`() = runTest {
        val api = FakeGroupEventsApi(events = emptyList())
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertEquals("1970-01-01T00:00:00Z", api.lastSince)
    }

    @Test
    fun `sync without a session does not call the API`() = runTest {
        val api = FakeGroupEventsApi(events = emptyList())
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(initialSession = null))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertNull("without a session, the call shouldn't even happen", api.lastGroupId)
    }

    @Test
    fun `sync ignores an event of unknown type without breaking`() = runTest {
        val notificationRepository = FakeNotificationRepository()
        val api = FakeGroupEventsApi(events = listOf(GroupEventDto(type = 99, groupId = "remote-g1")))
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session), notificationRepository)

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertTrue(notificationRepository.inserted.isEmpty())
    }

    @Test
    fun `sync swallows a network failure without propagating it`() = runTest {
        val api = FakeGroupEventsApi(failure = { IOException("no connection") })
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1") // should not throw
    }

    @Test
    fun `sync swallows an HTTP error (403 or 404 group removed) without propagating it`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val api = FakeGroupEventsApi(failure = { HttpException(Response.error<Unit>(404, errorBody)) })
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1") // should not throw
    }

    private class FakeGroupEventsApi(
        private val events: List<GroupEventDto> = emptyList(),
        private val failure: (() -> Throwable)? = null,
    ) : GroupEventsApi {
        var lastBearerToken: String? = null
            private set
        var lastGroupId: String? = null
            private set
        var lastSince: String? = null
            private set

        override suspend fun getEvents(bearerToken: String, groupId: String, since: String): List<GroupEventDto> {
            lastBearerToken = bearerToken
            lastGroupId = groupId
            lastSince = since
            failure?.invoke()?.let { throw it }
            return events
        }
    }

    private class FakeTokenStorage(initialSession: AuthSession?) : TokenStorage {
        private var stored = initialSession
        override fun read(): AuthSession? = stored
        override fun save(session: AuthSession) {
            stored = session
        }

        override fun clear() {
            stored = null
        }
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

    private class FakeNotificationRepository(
        val inserted: MutableList<GroupNotification> = mutableListOf(),
        existing: MutableList<GroupNotification> = mutableListOf(),
    ) : NotificationRepository {
        private val all = existing

        override fun getNotificationsFlow(): Flow<List<GroupNotification>> = MutableStateFlow(all)
        override fun getUnreadCountFlow(): Flow<Int> = MutableStateFlow(all.count { !it.isRead })
        override suspend fun insert(notification: GroupNotification) {
            inserted += notification
            all += notification
        }

        override suspend fun markAllAsRead() = throw UnsupportedOperationException("not used in this test")
        override suspend fun getLastEventTimestamp(): Instant? = all.maxOfOrNull { it.occurredAt }
    }
}
