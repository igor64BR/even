package com.tally.app.ui.creategroup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.repository.RoomGroupRepository
import com.tally.data.repository.RoomParticipantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers client-side validation and local persistence via Room. Same Robolectric
 * pattern as `GroupListViewModelTest`: an in-memory Room database, no emulator, real DAOs
 * behind [RoomGroupRepository]/[RoomParticipantRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TallyDatabase
    private lateinit var viewModel: CreateGroupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TallyDatabase::class.java,
        ).build()
        viewModel = CreateGroupViewModel(
            groupRepository = RoomGroupRepository(database.groupDao()),
            participantRepository = RoomParticipantRepository(database.participantDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the form starts with a fixed, non-removable You`() {
        val participants = viewModel.uiState.value.participants

        assertEquals(1, participants.size)
        assertTrue(participants.single().isYou)
    }

    @Test
    fun `saving with no name marks an error and persists nothing`() = runTest(testDispatcher) {
        viewModel.onNewParticipantNameChanged("Marina")
        viewModel.onAddParticipant()

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.nameError)
        assertTrue(database.groupDao().getGroupsFlow().first().isEmpty())
    }

    @Test
    fun `saving with fewer than 2 participants marks an error and persists nothing`() = runTest(testDispatcher) {
        viewModel.onNameChanged("Saturday barbecue")

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.participantsError)
        assertTrue(database.groupDao().getGroupsFlow().first().isEmpty())
    }

    @Test
    fun `removing You has no effect`() {
        val youId = viewModel.uiState.value.participants.single().id

        viewModel.onRemoveParticipant(youId)

        assertEquals(1, viewModel.uiState.value.participants.size)
    }

    @Test
    fun `saving a valid group persists the group and participants to Room and emits an event`() = runTest(testDispatcher) {
        var eventEmitted = false
        val collectorJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { eventEmitted = true }
        }

        viewModel.onNameChanged("Saturday barbecue")
        viewModel.onCategorySelected(GroupCategory.BARBECUE)
        viewModel.onNewParticipantNameChanged("Marina")
        viewModel.onAddParticipant()

        viewModel.onSaveClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val groups = database.groupDao().getGroupsFlow().first()
        assertEquals(1, groups.size)
        assertEquals("Saturday barbecue", groups.single().name)
        assertFalse("a locally created group starts out unsynced", groups.single().isSynced)

        val participants = database.participantDao().getParticipantsFlow(groups.single().id).first()
        assertEquals(2, participants.size)
        assertTrue(participants.any { it.name == "You" && it.isYou })
        assertTrue(participants.any { it.name == "Marina" && !it.isYou })

        assertTrue("CreateGroupEvent.GroupCreated should have been emitted", eventEmitted)
        collectorJob.cancel()
    }

    /**
     * Regression: `onSaveClick` set `isSaving = true` and never went back to `false` after
     * `insertGroup` finished — harmless while the screen unmounted when navigating back to "Your
     * groups" right after, but it turned into a stuck "Create group" button
     * (`enabled = !uiState.isSaving`) forever as soon as the same `ViewModel` was reused on a
     * subsequent visit (real bug reported: the reopened form wouldn't allow saving even with every
     * field filled in).
     */
    @Test
    fun `isSaving goes back to false after saving successfully`() = runTest(testDispatcher) {
        viewModel.onNameChanged("Saturday barbecue")
        viewModel.onNewParticipantNameChanged("Marina")
        viewModel.onAddParticipant()

        viewModel.onSaveClick()
        // A real suspension (not `advanceUntilIdle()` alone): `saveGroup` writes to Room via a
        // real executor, outside the `testDispatcher` (the same race documented in
        // `CreateExpenseViewModelTest`) — only really suspending until the event guarantees the
        // `finally` that resets `isSaving` (which runs BEFORE the emit, in the same coroutine body)
        // has already happened.
        viewModel.events.first()

        assertFalse("isSaving should go back to false after saving, otherwise the button gets stuck", viewModel.uiState.value.isSaving)
    }
}
