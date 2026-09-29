package com.tally.app.ui.notifications

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.persistence.entity.GroupEntity
import com.tally.data.persistence.entity.NotificationEntity
import com.tally.data.repository.RoomNotificationRepository
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser
import com.tally.domain.repository.AuthRepository
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
 * Covers T41.1: "requires account" state for a signed-out user, empty list, list with
 * notifications (faithful to `prototype/notifications.html`), and the unread badge (T41.2). Same
 * Robolectric pattern as the other screens — in-memory Room, real DAO behind
 * [RoomNotificationRepository].
 *
 * Always collects via [kotlinx.coroutines.flow.first] (never reads `.value` directly): the
 * `StateFlow`s in [NotificationsViewModel] use `SharingStarted.WhileSubscribed`, so the upstream
 * only starts being collected once someone subscribes — reading `.value` without ever having
 * subscribed would stay stuck at `initialValue` forever, even after `advanceUntilIdle()`.
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
    private lateinit var database: TallyDatabase
    private lateinit var notificationRepository: RoomNotificationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TallyDatabase::class.java,
        ).build()
        notificationRepository = RoomNotificationRepository(database.notificationDao())
        runBlocking { database.groupDao().insert(GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 500L)) }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(authRepository: AuthRepository) =
        NotificationsViewModel(notificationRepository = notificationRepository, authRepository = authRepository)

    @Test
    fun `without an authenticated session, state is RequiresAccount`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeAuthRepository(initialSession = null))

        val state = viewModel.uiState.first { it !is NotificationsUiState.Loading }

        assertEquals(NotificationsUiState.RequiresAccount, state)
    }

    @Test
    fun `authenticated with no notifications in Room, state is Empty`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val state = viewModel.uiState.first { it !is NotificationsUiState.Loading }

        assertEquals(NotificationsUiState.Empty, state)
    }

    @Test
    fun `authenticated with notifications in Room, Content lists them ordered by most recent`() = runTest(testDispatcher) {
        database.notificationDao().insert(
            NotificationEntity(id = "n1", groupId = "g1", message = "older", occurredAtEpochMillis = 1_000L, isRead = true),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "n2", groupId = "g1", message = "newer", occurredAtEpochMillis = 9_000L, isRead = false),
        )
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val state = viewModel.uiState.first { it is NotificationsUiState.Content } as NotificationsUiState.Content

        assertEquals(listOf("newer", "older"), state.notifications.map { it.message })
        assertTrue(state.notifications.first { it.message == "newer" }.isRead.not())
    }

    @Test
    fun `unreadNotificationsCount reflects unread notifications`() = runTest(testDispatcher) {
        database.notificationDao().insert(
            NotificationEntity(id = "n1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        val viewModel = buildViewModel(FakeAuthRepository(authenticatedSession))

        val count = viewModel.unreadNotificationsCount.first { it == 1 }

        assertEquals(1, count)
    }

    @Test
    fun `onScreenClosed marks all notifications as read`() = runTest(testDispatcher) {
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
            throw UnsupportedOperationException("not used in this test")

        override suspend fun signOut() {
            session.value = null
        }
    }
}
