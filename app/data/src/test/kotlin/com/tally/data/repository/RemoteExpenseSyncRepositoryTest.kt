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
 * Covers edit/delete expense against the backend (`PUT`/`DELETE
 * /groups/{id}/expenses/{expenseId}`) — see the documented pending item in
 * [com.tally.domain.repository.RemoteExpenseRepository]. Same simple test-double pattern as
 * [RemoteGroupSyncRepositoryTest]: no mocking framework, reusing [FakeGroupsApi]/
 * [FakeTokenStorage] already used there.
 */
class RemoteExpenseSyncRepositoryTest {

    private val expense = Expense(
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
    )
    private val session = AuthSession(
        accessToken = "valid-access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `updateExpense sends Authorization Bearer and the mapped expense payload`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)

        assertEquals("Bearer valid-access-token", groupsApi.lastBearerToken)
        assertEquals("remote-g1", groupsApi.lastGroupId)
        assertEquals("e1", groupsApi.lastExpenseId)
        val request = requireNotNull(groupsApi.lastUpdateRequest)
        assertEquals("Barbecue", request.description)
        assertEquals(10_000L, request.totalAmountCents)
        assertEquals(2, request.splits.size)
    }

    @Test
    fun `updateExpense without a session throws GroupSyncException without calling the backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("should not even try to call the backend without a session", groupsApi.lastUpdateRequest)
        }
    }

    @Test
    fun `updateExpense translates a network failure into GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("no connection to the Tally server") })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `updateExpense translates an HTTP error into GroupSyncException`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, errorBody)) })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    @Test
    fun `deleteExpense sends Authorization Bearer with the right ids`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")

        assertEquals("Bearer valid-access-token", groupsApi.lastBearerToken)
        assertEquals("remote-g1", groupsApi.lastGroupId)
        assertEquals("e1", groupsApi.lastExpenseId)
    }

    @Test
    fun `deleteExpense without a session throws GroupSyncException without calling the backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("should not even try to call the backend without a session", groupsApi.lastExpenseId)
        }
    }

    @Test
    fun `deleteExpense translates a network failure into GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("no connection to the Tally server") })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")
            fail("expected GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    private class FakeGroupsApi(private val failure: (() -> Throwable)? = null) : GroupsApi {
        var lastBearerToken: String? = null
            private set
        var lastGroupId: String? = null
            private set
        var lastExpenseId: String? = null
            private set
        var lastUpdateRequest: SyncedExpenseDto? = null
            private set

        override suspend fun sync(
            bearerToken: String,
            request: SyncGroupRequestDto,
        ): SyncGroupResponseDto = throw UnsupportedOperationException("not used in this test")

        override suspend fun join(bearerToken: String, code: String): SyncGroupResponseDto =
            throw UnsupportedOperationException("not used in this test")

        override suspend fun updateExpense(
            bearerToken: String,
            id: String,
            expenseId: String,
            request: SyncedExpenseDto,
        ) {
            lastBearerToken = bearerToken
            lastGroupId = id
            lastExpenseId = expenseId
            lastUpdateRequest = request
            failure?.invoke()?.let { throw it }
        }

        override suspend fun deleteExpense(bearerToken: String, id: String, expenseId: String) {
            lastBearerToken = bearerToken
            lastGroupId = id
            lastExpenseId = expenseId
            failure?.invoke()?.let { throw it }
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
}
