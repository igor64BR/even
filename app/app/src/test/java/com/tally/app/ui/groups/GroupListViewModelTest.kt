package com.tally.app.ui.groups

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.persistence.entity.GroupEntity
import com.tally.data.persistence.entity.NotificationEntity
import com.tally.data.persistence.entity.ParticipantEntity
import com.tally.data.repository.RoomGroupRepository
import com.tally.data.repository.RoomNotificationRepository
import com.tally.data.repository.RoomParticipantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Inserts data directly through the real DAOs (same Room used in production) and confirms that
 * [GroupListViewModel] combines [RoomGroupRepository] + [RoomParticipantRepository] into the
 * right [GroupListUiState]: no groups -> Empty; with a group -> Content with a reactive
 * participant count. Same Robolectric pattern as `:data`'s `TallyDatabaseTest`, in-memory
 * database, no disk access or emulator needed.
 *
 * The "Sync this group" action and its tests live in
 * [com.tally.app.ui.groupdetail.GroupDetailViewModelTest] — this class covers only what
 * [GroupListViewModel] actually does today, plus `unreadNotificationsCount` (badge for the
 * "Notifications" tab).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GroupListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TallyDatabase
    private lateinit var viewModel: GroupListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TallyDatabase::class.java,
        ).build()
        viewModel = GroupListViewModel(
            groupRepository = RoomGroupRepository(database.groupDao()),
            participantRepository = RoomParticipantRepository(database.participantDao()),
            notificationRepository = RoomNotificationRepository(database.notificationDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `no groups in Room, state is Empty`() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { it !is GroupListUiState.Loading }

        assertEquals(GroupListUiState.Empty, state)
    }

    @Test
    fun `group with participants in Room shows up in Content with the right count`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "bbq", name = "Saturday BBQ", createdAtEpochMillis = 1_000L),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "you", groupId = "bbq", name = "You", isYou = true),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "marina", groupId = "bbq", name = "Marina", isYou = false),
        )

        val state = viewModel.uiState.first { it is GroupListUiState.Content } as GroupListUiState.Content

        val group = state.groups.single()
        assertEquals("Saturday BBQ", group.name)
        assertEquals(2, group.participantCount)
        assertEquals("SA", group.tag)
        assertTrue(group.balance is GroupBalance.Settled)
        assertTrue("a group inserted without an explicit isSynced starts out local (isSynced=false)", !group.isSynced)
    }

    @Test
    fun `synced group in Room shows up with isSynced true in Content`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "trip", name = "Trip", createdAtEpochMillis = 2_000L, isSynced = true),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "you", groupId = "trip", name = "You", isYou = true),
        )

        val state = viewModel.uiState.first { it is GroupListUiState.Content } as GroupListUiState.Content

        val group = state.groups.single()
        assertTrue(
            "GroupListViewModel.toUiModel() must pass through the real Group.isSynced value",
            group.isSynced,
        )
    }

    @Test
    fun `unread badge reflects NotificationEntity with isRead false`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "bbq", name = "Saturday BBQ", createdAtEpochMillis = 1_000L),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "bbq", message = "Marina added an expense.", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "settlement:s1", groupId = "bbq", message = "You settled something.", occurredAtEpochMillis = 2_000L, isRead = true),
        )

        val count = viewModel.unreadNotificationsCount.first { it == 1 }

        assertEquals(1, count)
    }
}
