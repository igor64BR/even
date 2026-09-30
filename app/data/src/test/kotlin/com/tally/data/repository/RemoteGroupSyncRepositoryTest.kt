package com.tally.data.repository

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.groups.GroupsApi
import com.tally.data.remote.groups.SyncGroupRequestDto
import com.tally.data.remote.groups.SyncGroupResponseDto
import com.tally.data.remote.groups.SyncedExpenseDto
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Group
import com.tally.domain.model.Participant
import com.tally.domain.repository.GroupSyncException
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * [RemoteGroupSyncRepository] mirrors `POST /groups/sync` with the
 * `Authorization: Bearer` read from [TokenStorage] and translates network/HTTP failure (or a
 * missing session) into [GroupSyncException] — never lets `HttpException`/`IOException` leak out.
 * Same pattern as `RemoteAuthRepositoryTest`: simple test doubles for [GroupsApi]/[TokenStorage]
 * instead of a mocking framework (none is in `:data`'s test dependencies).
 */
class RemoteGroupSyncRepositoryTest {

    private val group = Group(
        id = "g1",
        name = "Saturday barbecue",
        createdAt = Instant.parse("2026-01-10T12:00:00Z"),
    )
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "You", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Marina"),
    )
    private val expenses = listOf(
        Expense(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 10_000,
            paidByParticipantId = "p1",
            createdAt = Instant.parse("2026-01-11T12:00:00Z"),
            splits = listOf(
                ExpenseSplit.Equal(participantId = "p1"),
                ExpenseSplit.Equal(participantId = "p2"),
            ),
        ),
    )
    private val session = AuthSession(
        accessToken = "valid-access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `syncGroup sends Authorization Bearer with the session access token`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.syncGroup(group, participants, expenses)

        assertEquals("Bearer valid-access-token", groupsApi.lastBearerToken)
    }

    @Test
    fun `syncGroup maps group, participants and expenses to the backend payload`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        val remoteId = repository.syncGroup(group, participants, expenses)

        assertEquals("remote-group-id", remoteId)
        val request = requireNotNull(groupsApi.lastRequest)
        assertEquals("Saturday barbecue", request.name)
        assertEquals(listOf("p1", "p2"), request.participants.map { it.id })
        assertTrue("every local participant syncs as a guest", request.participants.all { it.isGuest })
        assertEquals(1, request.expenses.size)
        val expense = request.expenses.single()
        assertEquals("2026-01-11", expense.date)
        assertEquals(2, expense.splits.size)
    }

    @Test
    fun `syncGroup without a session throws GroupSyncException without calling the backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("should not even try to call the backend without a session", groupsApi.lastRequest)
        }
    }

    @Test
    fun `syncGroup translates a network failure into GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("no connection to the Tally server") })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `syncGroup translates an HTTP error into GroupSyncException`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, errorBody)) })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    @Test
    fun `joinByCode sends Authorization Bearer and the invite code`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        val remoteId = repository.joinByCode("ABC123")

        assertEquals("Bearer valid-access-token", groupsApi.lastBearerToken)
        assertEquals("ABC123", groupsApi.lastCode)
        assertEquals("remote-group-id", remoteId)
    }

    @Test
    fun `joinByCode without a session throws GroupSyncException without calling the backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.joinByCode("ABC123")
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("should not even try to call the backend without a session", groupsApi.lastCode)
        }
    }

    @Test
    fun `joinByCode translates a network failure into GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("no connection to the Tally server") })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.joinByCode("ABC123")
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `joinByCode translates an invalid code (HTTP error) into GroupSyncException`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, errorBody)) })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.joinByCode("INVALID-CODE")
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    private class FakeGroupsApi(private val failure: (() -> Throwable)? = null) : GroupsApi {
        var lastBearerToken: String? = null
            private set
        var lastRequest: SyncGroupRequestDto? = null
            private set
        var lastCode: String? = null
            private set

        override suspend fun sync(
            bearerToken: String,
            request: SyncGroupRequestDto,
        ): SyncGroupResponseDto {
            lastBearerToken = bearerToken
            lastRequest = request
            failure?.invoke()?.let { throw it }
            return SyncGroupResponseDto(groupId = "remote-group-id")
        }

        override suspend fun join(bearerToken: String, code: String): SyncGroupResponseDto {
            lastBearerToken = bearerToken
            lastCode = code
            failure?.invoke()?.let { throw it }
            return SyncGroupResponseDto(groupId = "remote-group-id")
        }

        // Not used by RemoteGroupSyncRepositoryTest (RemoteExpenseSyncRepositoryTest covers these)
        // — only here to satisfy the GroupsApi interface.
        override suspend fun updateExpense(
            bearerToken: String,
            id: String,
            expenseId: String,
            request: SyncedExpenseDto,
        ): Unit = throw UnsupportedOperationException("not used in this test")

        override suspend fun deleteExpense(bearerToken: String, id: String, expenseId: String): Unit =
            throw UnsupportedOperationException("not used in this test")
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
}
