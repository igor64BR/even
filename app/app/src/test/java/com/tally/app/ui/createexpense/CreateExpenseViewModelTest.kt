package com.tally.app.ui.createexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.repository.RoomExpenseRepository
import com.tally.data.repository.RoomGroupRepository
import com.tally.data.repository.RoomParticipantRepository
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Group
import com.tally.domain.model.Participant
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.RemoteExpenseRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the live calculation of the equal split and the local persistence + validation of the
 * "New expense" form. Same Robolectric pattern as `CreateGroupViewModelTest`: an in-memory Room
 * database, real DAOs behind `Room*Repository`, group + participants seeded directly through the
 * repository (there's no group detail screen to seed through the UI).
 *
 * The initial participant load ([CreateExpenseViewModel.init]) and the expense write
 * ([CreateExpenseViewModel.onSaveClick]) run on Room's real `TransactionExecutor`, outside the
 * [StandardTestDispatcher] — `advanceUntilIdle()` alone doesn't wait for that real work to finish
 * (the same race documented in `GroupListViewModelTest`, "sync successfully"). That's why the
 * tests wait by really suspending on `Flow.first { predicate }` (on
 * [CreateExpenseViewModel.uiState]/[CreateExpenseViewModel.events]/the repository), never just
 * reading `.value` after `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TallyDatabase
    private lateinit var groupRepository: RoomGroupRepository
    private lateinit var participantRepository: RoomParticipantRepository
    private lateinit var expenseRepository: RoomExpenseRepository
    private lateinit var groupId: String

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TallyDatabase::class.java,
        ).build()
        groupRepository = RoomGroupRepository(database.groupDao())
        participantRepository = RoomParticipantRepository(database.participantDao())
        expenseRepository = RoomExpenseRepository(database.expenseDao())

        groupId = "group-1"
        groupRepository.insertGroup(Group(id = groupId, name = "Saturday barbecue", createdAt = Instant.now()))
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun seedParticipants(vararg participants: Participant) {
        participants.forEach { participantRepository.insertParticipant(it) }
    }

    /**
     * Creates the ViewModel and really suspends until the initial participant load arrives.
     * [expenseId] switches on edit mode — `null` (default) keeps the usual behavior (create
     * mode). [remoteExpenseRepository] only matters for the tests about propagating the edit
     * to the backend (a synced group); the others use the default fake, which is never called
     * because no group seeded here has `isSynced = true`.
     */
    private suspend fun createViewModelWithParticipantsLoaded(
        expenseId: String? = null,
        remoteExpenseRepository: RemoteExpenseRepository = FakeRemoteExpenseRepository(),
    ): CreateExpenseViewModel {
        val viewModel = CreateExpenseViewModel(
            groupId = groupId,
            expenseId = expenseId,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            groupRepository = groupRepository,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.participants.isNotEmpty() }
        return viewModel
    }

    @Test
    fun `equal split among 3 participants matches the exact total even without integer division`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onAmountChanged("10,00")

        val rows = viewModel.uiState.value.splitRows
        assertEquals(3, rows.size)
        assertEquals(1000L, rows.sumOf { it.amountCents })
        // "p1" is first in participantId order, gets the extra cent (334+333+333=1000).
        assertEquals(334L, rows.single { it.participantId == "p1" }.amountCents)
        assertEquals(333L, rows.single { it.participantId == "p2" }.amountCents)
        assertEquals(333L, rows.single { it.participantId == "p3" }.amountCents)
    }

    @Test
    fun `unchecking a participant recalculates the split only among the remaining ones`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onAmountChanged("10,00")
        viewModel.onParticipantToggled("p2")

        val rows = viewModel.uiState.value.splitRows
        assertFalse(rows.single { it.participantId == "p2" }.isIncluded)
        assertEquals(0L, rows.single { it.participantId == "p2" }.amountCents)
        assertEquals(1000L, rows.single { it.participantId == "p1" }.amountCents)
    }

    @Test
    fun `saving with no description marks an error and persists nothing`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onAmountChanged("10,00")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.descriptionError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `saving with a zero amount marks an error and persists nothing`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("0")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.amountError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `saving with no participant selected marks an error and persists nothing`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("10,00")
        viewModel.onParticipantToggled("p1")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.participantsError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `saving a valid expense persists to Room with Equal splits and emits an event`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("10,00")
        viewModel.onPayerSelected("p2")
        viewModel.onSaveClick()

        // A real suspension: only returns after the expense is persisted and the event emitted
        // (see the class note) — no advanceUntilIdle() alone here.
        viewModel.events.first()

        val expenses = expenseRepository.getExpensesFlow(groupId).first()
        assertEquals(1, expenses.size)
        val expense = expenses.single()
        assertEquals("Friday dinner", expense.description)
        assertEquals(1000L, expense.amountCents)
        assertEquals("p2", expense.paidByParticipantId)
        assertEquals(3, expense.splits.size)
        assertTrue(expense.splits.all { it is ExpenseSplit.Equal })
        assertEquals(setOf("p1", "p2", "p3"), expense.splits.map { it.participantId }.toSet())
    }

    // --- Percentage tab ---

    @Test
    fun `saving with a percentage that does not add up to 100 percent persists nothing`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.PERCENTAGE)
        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("100,00")
        viewModel.onPercentageChanged("p1", "40")
        viewModel.onPercentageChanged("p2", "40")
        viewModel.onSaveClick()

        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `saving an expense with a percentage split persists the corresponding Weight splits`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.PERCENTAGE)
        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("100,00")
        viewModel.onPayerSelected("p1")
        viewModel.onPercentageChanged("p1", "60")
        viewModel.onPercentageChanged("p2", "40")
        viewModel.onSaveClick()

        viewModel.events.first()

        val expense = expenseRepository.getExpensesFlow(groupId).first().single()
        assertEquals(2, expense.splits.size)
        assertTrue(expense.splits.all { it is ExpenseSplit.Weight })
        val weights = expense.splits.associate { it.participantId to (it as ExpenseSplit.Weight).weight }
        assertEquals(60L, weights.getValue("p1"))
        assertEquals(40L, weights.getValue("p2"))
    }

    // --- Fixed amount tab ---

    @Test
    fun `saving with a fixed amount that does not match the total persists nothing`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.FIXED_AMOUNT)
        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("100,00")
        viewModel.onFixedAmountChanged("p1", "40,00")
        viewModel.onFixedAmountChanged("p2", "40,00")
        viewModel.onSaveClick()

        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `saving an expense with a fixed-amount split persists the corresponding FixedAmount splits`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.FIXED_AMOUNT)
        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("100,00")
        viewModel.onPayerSelected("p1")
        viewModel.onFixedAmountChanged("p1", "70,00")
        viewModel.onFixedAmountChanged("p2", "30,00")
        viewModel.onSaveClick()

        viewModel.events.first()

        val expense = expenseRepository.getExpensesFlow(groupId).first().single()
        assertEquals(2, expense.splits.size)
        assertTrue(expense.splits.all { it is ExpenseSplit.FixedAmount })
        val amounts = expense.splits.associate { it.participantId to (it as ExpenseSplit.FixedAmount).amount.cents }
        assertEquals(7000L, amounts.getValue("p1"))
        assertEquals(3000L, amounts.getValue("p2"))
    }

    // --- Edit mode ---

    private suspend fun seedExpense(): Expense {
        val expense = Expense(
            id = "e1",
            groupId = groupId,
            description = "Charcoal and meat",
            amountCents = 10_00,
            paidByParticipantId = "p1",
            createdAt = Instant.parse("2026-03-01T12:00:00Z"),
            splits = listOf(
                ExpenseSplit.Equal(participantId = "p1"),
                ExpenseSplit.Equal(participantId = "p2"),
            ),
        )
        expenseRepository.insertExpense(expense)
        return expense
    }

    @Test
    fun `opening in edit mode pre-fills description amount payer and split of the existing expense`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val expense = seedExpense()

        val viewModel = createViewModelWithParticipantsLoaded(expenseId = expense.id)
        val state = viewModel.uiState.first { it.description == "Charcoal and meat" }

        assertTrue("uiState.expenseId should point to the expense being edited", state.isEditMode)
        assertEquals("10,00", state.amountInput)
        assertEquals("p1", state.payerId)
        assertEquals(SplitMode.EQUAL, state.splitMode)
        assertTrue(state.splitRows.single { it.participantId == "p1" }.isIncluded)
        assertTrue(state.splitRows.single { it.participantId == "p2" }.isIncluded)
        assertFalse(
            "p3 was not in the original expense, should not show up checked when reopened for editing",
            state.splitRows.single { it.participantId == "p3" }.isIncluded,
        )
    }

    @Test
    fun `saving an edit replaces the existing expense in Room keeping the same id`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        val viewModel = createViewModelWithParticipantsLoaded(expenseId = expense.id)
        viewModel.uiState.first { it.description == "Charcoal and meat" }

        viewModel.onDescriptionChanged("Charcoal, meat and ice")
        viewModel.onAmountChanged("20,00")
        viewModel.onSaveClick()
        viewModel.events.first()

        val expenses = expenseRepository.getExpensesFlow(groupId).first()
        assertEquals("editing replaces the expense (upsert), never duplicates it", 1, expenses.size)
        val updated = expenses.single()
        assertEquals("e1", updated.id)
        assertEquals("Charcoal, meat and ice", updated.description)
        assertEquals(2000L, updated.amountCents)
    }

    @Test
    fun `saving an edit for an unsynced group does not call the backend`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Charcoal and meat" }

        viewModel.onDescriptionChanged("Charcoal, meat and ice")
        viewModel.onSaveClick()
        viewModel.events.first()

        assertNull("a local (unsynced) group should never try to talk to the backend", remoteExpenseRepository.lastUpdatedExpense)
    }

    /**
     * Marks the group as synced by reinserting the same row with `isSynced=true` — the same
     * production path (`GroupDetailViewModel.syncGroup`). `GroupDao.insert` is `@Upsert`
     * (fixed separately: `@Insert(OnConflictStrategy.REPLACE)` made SQLite
     * delete+reinsert the row, triggering `ON DELETE CASCADE` and wiping out participants/expenses
     * along with it), so this doesn't erase what was already seeded.
     */
    private suspend fun markGroupAsSynced(remoteId: String) {
        val group = requireNotNull(groupRepository.getGroupById(groupId))
        groupRepository.insertGroup(group.copy(isSynced = true, remoteId = remoteId))
    }

    @Test
    fun `saving an edit for a synced group propagates the change to the backend`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        markGroupAsSynced(remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Charcoal and meat" }

        viewModel.onDescriptionChanged("Charcoal, meat and ice")
        viewModel.onSaveClick()
        viewModel.events.first()

        assertEquals("remote-$groupId", remoteExpenseRepository.lastRemoteGroupId)
        assertEquals("Charcoal, meat and ice", remoteExpenseRepository.lastUpdatedExpense?.description)
    }

    @Test
    fun `a network failure while propagating an edit does not undo the local change or block the saved event`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "You", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        markGroupAsSynced(remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository(
            failure = { GroupSyncException("No connection to the Tally server.") },
        )
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Charcoal and meat" }

        viewModel.onDescriptionChanged("Charcoal, meat and ice")
        viewModel.onSaveClick()

        // A real suspension: the event arrives even though the remote propagation fails.
        viewModel.events.first()

        val updated = expenseRepository.getExpensesFlow(groupId).first().single()
        assertEquals("the local change stands regardless of the network failure (local-first)", "Charcoal, meat and ice", updated.description)
    }

    /**
     * Regression: `onSaveClick` set `isSaving = true` and never went back to `false` after the
     * insert/update finished — harmless while the screen unmounted when navigating back to the
     * group right after, but it turned into a stuck "Save expense" button
     * (`enabled = !uiState.isSaving`) forever as soon as the same `ViewModel` was reused on a
     * subsequent visit (real bug reported: logging a second expense reopened the form unable to
     * save, even when filled in).
     */
    @Test
    fun `isSaving goes back to false after saving successfully`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Friday dinner")
        viewModel.onAmountChanged("10,00")
        viewModel.onSaveClick()
        viewModel.events.first()

        assertFalse("isSaving should go back to false after saving, otherwise the button gets stuck", viewModel.uiState.value.isSaving)
    }

    private class FakeRemoteExpenseRepository(private val failure: (() -> Throwable)? = null) : RemoteExpenseRepository {
        var lastRemoteGroupId: String? = null
            private set
        var lastUpdatedExpense: Expense? = null
            private set

        override suspend fun updateExpense(remoteGroupId: String, expense: Expense) {
            lastRemoteGroupId = remoteGroupId
            lastUpdatedExpense = expense
            failure?.invoke()?.let { throw it }
        }

        override suspend fun deleteExpense(remoteGroupId: String, expenseId: String) {
            throw UnsupportedOperationException("not used in this test")
        }
    }
}
