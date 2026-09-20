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
 * Cobre T22.1/T22.2 (deep link `rateio://join/{codigo}` + tela de confirmação): a decisão de
 * "logado entra direto" vs. "deslogado precisa de login primeiro" vem inteira de
 * [AuthRepository.getSessionFlow] (mesma fonte de verdade de [com.rateio.app.ui.auth.AuthViewModel],
 * T12, e [com.rateio.app.ui.groupdetail.GroupDetailViewModel], T19/T42.4) — o ViewModel nunca
 * navega sozinho, só expõe [JoinGroupUiState.NeedsLogin] pra quem observa decidir. Dublês simples
 * de [AuthRepository]/[RemoteGroupRepository], mesmo padrão de `GroupDetailViewModelTest`.
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
    fun `link com usuario logado mostra confirmacao direto, sem passar por login`() = runTest(testDispatcher) {
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = FakeRemoteGroupRepository(),
        )

        val state = viewModel.uiState.first { it !is JoinGroupUiState.CheckingSession }

        assertEquals(JoinGroupUiState.Confirming("ABC123"), state)
    }

    @Test
    fun `confirmar com usuario logado chama joinByCode e expoe sucesso com o id remoto`() = runTest(testDispatcher) {
        val remoteGroupRepository = FakeRemoteGroupRepository(remoteGroupId = "grupo-remoto-9")
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = remoteGroupRepository,
        )
        viewModel.uiState.first { it is JoinGroupUiState.Confirming }

        viewModel.confirm()

        val state = viewModel.uiState.first { it is JoinGroupUiState.Success }
        assertEquals(JoinGroupUiState.Success("grupo-remoto-9"), state)
        assertEquals("ABC123", remoteGroupRepository.lastInviteCode)
    }

    @Test
    fun `link com usuario deslogado expoe NeedsLogin com o codigo do convite, sem chamar o backend`() = runTest(testDispatcher) {
        val remoteGroupRepository = FakeRemoteGroupRepository()
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = null),
            remoteGroupRepository = remoteGroupRepository,
        )

        val state = viewModel.uiState.first { it !is JoinGroupUiState.CheckingSession }

        assertEquals(JoinGroupUiState.NeedsLogin("ABC123"), state)
        assertTrue("NeedsLogin nao pode ter chamado joinByCode", remoteGroupRepository.lastInviteCode == null)
    }

    @Test
    fun `login apos NeedsLogin retoma o fluxo e mostra confirmacao com o mesmo codigo`() = runTest(testDispatcher) {
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
    fun `falha ao entrar traduz GroupSyncException para Error com o codigo do convite`() = runTest(testDispatcher) {
        val viewModel = JoinGroupViewModel(
            inviteCode = "ABC123",
            authRepository = FakeAuthRepository(initialSession = session),
            remoteGroupRepository = FakeRemoteGroupRepository(
                failure = { GroupSyncException("Não foi possível entrar nesse grupo — verifique o código.") },
            ),
        )
        viewModel.uiState.first { it is JoinGroupUiState.Confirming }

        viewModel.confirm()

        val state = viewModel.uiState.first { it is JoinGroupUiState.Error } as JoinGroupUiState.Error
        assertEquals("ABC123", state.inviteCode)
        assertEquals("Não foi possível entrar nesse grupo — verifique o código.", state.message)
    }

    private class FakeAuthRepository(initialSession: AuthSession?) : AuthRepository {
        private val session = MutableStateFlow(initialSession)

        fun signIn(session: AuthSession) {
            this.session.value = session
        }

        override fun getSessionFlow(): Flow<AuthSession?> = session

        override suspend fun signInWithGoogle(googleIdToken: String): AuthSession =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun signOut() {
            session.value = null
        }
    }

    private class FakeRemoteGroupRepository(
        private val remoteGroupId: String = "remote-grupo-id",
        private val failure: (() -> Throwable)? = null,
    ) : RemoteGroupRepository {
        var lastInviteCode: String? = null
            private set

        override suspend fun syncGroup(
            group: Group,
            participants: List<Participant>,
            expenses: List<Expense>,
        ): String = throw UnsupportedOperationException("não usado neste teste")

        override suspend fun joinByCode(inviteCode: String): String {
            lastInviteCode = inviteCode
            failure?.invoke()?.let { throw it }
            return remoteGroupId
        }
    }
}
