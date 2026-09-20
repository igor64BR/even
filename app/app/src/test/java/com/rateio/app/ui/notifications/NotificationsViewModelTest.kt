package com.rateio.app.ui.notifications

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.NotificationEntity
import com.rateio.data.repository.RoomNotificationRepository
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cobre T41.1: estado "exige conta" pra usuário deslogado, lista vazia, lista com notificações
 * (fiel a `prototype/notificacoes.html`), e o badge de não lidas (T41.2). Mesmo padrão Robolectric
 * das demais telas — Room em memória, DAO de verdade por trás de [RoomNotificationRepository].
 *
 * Sempre coleta via [kotlinx.coroutines.flow.first] (nunca lê `.value` direto): os `StateFlow`
 * de [NotificationsViewModel] usam `SharingStarted.WhileSubscribed`, então o upstream só começa a
 * ser coletado quando alguém assina — ler `.value` sem nunca ter assinado ficaria preso no
 * `initialValue` pra sempre, mesmo depois de `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotificationsViewModelTest {

    private val authenticatedSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
    private lateinit var notificationRepository: RoomNotificationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RateioDatabase::class.java,
        ).build()
        notificationRepository = RoomNotificationRepository(database.notificationDao())
        runBlocking { database.groupDao().insert(GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 500L)) }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(authRepository: AuthRepository) =
        NotificationsViewModel(notificationRepository = notificationRepository, authRepository = authRepository)

    @Test
    fun `sem sessao autenticada, estado e RequiresAccount`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeAuthRepository(initialSession = null))

        val state = viewModel.uiState.first { it !is NotificationsUiState.Loading }

        assertEquals(NotificationsUiState.RequiresAccount, state)
    }

    @Test
    fun `autenticado sem notificacoes no Room, estado e Empty`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val state = viewModel.uiState.first { it !is NotificationsUiState.Loading }

        assertEquals(NotificationsUiState.Empty, state)
    }

    @Test
    fun `autenticado com notificacoes no Room, Content lista ordenada por mais recente`() = runTest(testDispatcher) {
        database.notificationDao().insert(
            NotificationEntity(id = "n1", groupId = "g1", message = "mais antiga", occurredAtEpochMillis = 1_000L, isRead = true),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "n2", groupId = "g1", message = "mais nova", occurredAtEpochMillis = 9_000L, isRead = false),
        )
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val state = viewModel.uiState.first { it is NotificationsUiState.Content } as NotificationsUiState.Content

        assertEquals(listOf("mais nova", "mais antiga"), state.notifications.map { it.message })
        assertTrue(state.notifications.first { it.message == "mais nova" }.isRead.not())
    }

    @Test
    fun `unreadNotificationsCount reflete notificacoes nao lidas`() = runTest(testDispatcher) {
        database.notificationDao().insert(
            NotificationEntity(id = "n1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val count = viewModel.unreadNotificationsCount.first { it == 1 }

        assertEquals(1, count)
    }

    @Test
    fun `onScreenClosed marca todas as notificacoes como lidas`() = runTest(testDispatcher) {
        database.notificationDao().insert(
            NotificationEntity(id = "n1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))
        viewModel.unreadNotificationsCount.first { it == 1 }

        viewModel.onScreenClosed()

        val count = viewModel.unreadNotificationsCount.first { it == 0 }
        assertEquals(0, count)
    }

    private class FakeAuthRepository(initialSession: AuthSession?) : AuthRepository {
        private val session = MutableStateFlow(initialSession)

        override fun getSessionFlow(): Flow<AuthSession?> = session

        override suspend fun signInWithGoogle(googleIdToken: String): AuthSession =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun signOut() {
            session.value = null
        }
    }
}
