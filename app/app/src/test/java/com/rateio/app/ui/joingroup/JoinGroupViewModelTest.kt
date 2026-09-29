package com.rateio.app.ui.joingroup

import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.model.Expense
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers T22.1/T22.2 (deep link `rateio://join/{code}` + confirmation screen): the decision
 * between "signed in goes straight through" vs. "signed out needs to log in first" comes entirely
 * from [AuthRepository.getSessionFlow] (same source of truth as [com.rateio.app.ui.auth.AuthViewModel],
 * T12, and [com.rateio.app.ui.groupdetail.GroupDetailViewModel], T19/T42.4) — the ViewModel never
 * navigates on its own, it only exposes [JoinGroupUiState.NeedsLogin] for whoever observes it to
 * decide. Simple fakes for [AuthRepository]/[RemoteGroupRepository], same pattern as
 * `GroupDetailViewModelTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JoinGroupViewModelTest {

    private val session = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `link with a signed-in user shows confirmation directly, without going through login`() = runTest(testDispatcher) {
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = FakeRemoteGroupRepository(),
        )

        val state = viewModel.uiState.first { it !is JoinGroupUiState.CheckingSession }

        assertEquals(JoinGroupUiState.Confirming("ABC123"), state)
    }

    @Test
    fun `confirming with a signed-in user calls joinByCode and exposes success with the remote id`() = runTest(testDispatcher) {
        val remoteGroupRepository = FakeRemoteGroupRepository(remoteGroupId = "remote-group-9")
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = remoteGroupRepository,
        )
        viewModel.uiState.first { it is JoinGroupUiState.Confirming }

        viewModel.confirm()

        val state = viewModel.uiState.first { it is JoinGroupUiState.Success }
        assertEquals(JoinGroupUiState.Success("remote-group-9"), state)
        assertEquals("ABC123", remoteGroupRepository.lastInviteCode)
    }

    @Test
    fun `link with a signed-out user exposes NeedsLogin with the invite code, without calling the backend`() = runTest(testDispatcher) {
        val remoteGroupRepository = FakeRemoteGroupRepository()
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = null),
            remoteGroupRepository = remoteGroupRepository,
        )

        val state = viewModel.uiState.first { it !is JoinGroupUiState.CheckingSession }

        assertEquals(JoinGroupUiState.NeedsLogin("ABC123"), state)
        assertTrue("NeedsLogin must not have called joinByCode", remoteGroupRepository.lastInviteCode == null)
    }

    @Test
    fun `logging in after NeedsLogin resumes the flow and shows confirmation with the same code`() = runTest(testDispatcher) {
        val authRepository = FakeAuthRepository(initialSession = null)
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = authRepository,
            remoteGroupRepository = FakeRemoteGroupRepository(),
        )
        viewModel.uiState.first { it is JoinGroupUiState.NeedsLogin }

        authRepository.signIn(session)

        val state = viewModel.uiState.first { it is JoinGroupUiState.Confirming }
        assertEquals(JoinGroupUiState.Confirming("ABC123"), state)
    }

    @Test
    fun `failure to join translates GroupSyncException into Error with the invite code`() = runTest(testDispatcher) {
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = FakeRemoteGroupRepository(
                failure = { GroupSyncException("Could not join this group — check the code.") },
            ),
        )
        viewModel.uiState.first { it is JoinGroupUiState.Confirming }

        viewModel.confirm()

        val state = viewModel.uiState.first { it is JoinGroupUiState.Error } as JoinGroupUiState.Error
        assertEquals("ABC123", state.inviteCode)
        assertEquals("Could not join this group — check the code.", state.message)
    }

    private class FakeAuthRepository(initialSession: AuthSession?) : AuthRepository {
        private val session = MutableStateFlow(initialSession)

        fun signIn(session: AuthSession) {
            this.session.value = session
        }

        override fun getSessionFlow(): Flow<AuthSession?> = session

        override suspend fun signInWithGoogle(googleIdToken: String): AuthSession =
            throw UnsupportedOperationException("not used in this test")

        override suspend fun signOut() {
            session.value = null
        }
    }

    private class FakeRemoteGroupRepository(
        private val remoteGroupId: String = "remote-group-id",
        private val failure: (() -> Throwable)? = null,
    ) : RemoteGroupRepository {
        var lastInviteCode: String? = null
            private set

        override suspend fun syncGroup(
            group: Group,
            participants: List<Participant>,
            expenses: List<Expense>,
        ): String = throw UnsupportedOperationException("not used in this test")

        override suspend fun joinByCode(inviteCode: String): String {
            lastInviteCode = inviteCode
            failure?.invoke()?.let { throw it }
            return remoteGroupId
        }
    }
}
