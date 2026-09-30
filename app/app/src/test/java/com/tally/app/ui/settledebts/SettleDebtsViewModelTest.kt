package com.tally.app.ui.settledebts

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.repository.RoomExpenseRepository
import com.tally.data.repository.RoomGroupRepository
import com.tally.data.repository.RoomParticipantRepository
import com.tally.data.repository.RoomSettlementRepository
import com.tally.domain.engine.GreedyDebtSimplificationEngine
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Group
import com.tally.domain.model.Participant
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * `computeSettlement` over the current balances becomes the suggestion list;
 * "Mark as paid" writes a `Settlement` and the list recalculates on its own (it's derived
 * from the settlements Flow, not a cached state); a group with zeroed-out balances goes straight
 * into [SettleDebtsUiState.SettledUp]. Same Robolectric pattern as the other screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettleDebtsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TallyDatabase
    private lateinit var groupRepository: RoomGroupRepository
    private lateinit var participantRepository: RoomParticipantRepository
    private lateinit var expenseRepository: RoomExpenseRepository
    private lateinit var settlementRepository: RoomSettlementRepository
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
        settlementRepository = RoomSettlementRepository(database.settlementDao())

        groupId = "bbq"
        groupRepository.insertGroup(Group(id = groupId, name = "Saturday BBQ", createdAt = Instant.now()))
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SettleDebtsViewModel(
        groupId = groupId,
        groupRepository = groupRepository,
        participantRepository = participantRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        debtSimplificationEngine = GreedyDebtSimplificationEngine(),
    )

    /** A: -1000 (owes) · B: +1000 (is owed) — the simplest possible debt case. */
    private suspend fun seedSimpleDebt() {
        participantRepository.insertParticipant(Participant(id = "a", groupId = groupId, name = "Ana"))
        participantRepository.insertParticipant(Participant(id = "b", groupId = groupId, name = "Bruno"))
        expenseRepository.insertExpense(
            Expense(
                id = "e1",
                groupId = groupId,
                description = "Beer",
                amountCents = 2000,
                paidByParticipantId = "b",
                createdAt = Instant.EPOCH,
                splits = listOf(ExpenseSplit.Equal(participantId = "a"), ExpenseSplit.Equal(participantId = "b")),
            ),
        )
    }

    @Test
    fun `group with zeroed-out balances shows settled state`() = runTest(testDispatcher) {
        participantRepository.insertParticipant(Participant(id = "a", groupId = groupId, name = "Ana"))
        val viewModel = buildViewModel()

        val state = viewModel.uiState.first { it !is SettleDebtsUiState.Loading }

        assertTrue(state is SettleDebtsUiState.SettledUp)
        assertEquals("Saturday BBQ", (state as SettleDebtsUiState.SettledUp).groupName)
    }

    @Test
    fun `settlement suggestion uses computeSettlement over the current balances`() = runTest(testDispatcher) {
        seedSimpleDebt()
        val viewModel = buildViewModel()

        val state = viewModel.uiState.first { it is SettleDebtsUiState.Content } as SettleDebtsUiState.Content

        val suggestion = state.suggestions.single()
        assertEquals("a", suggestion.fromParticipantId)
        assertEquals("b", suggestion.toParticipantId)
        assertEquals("Ana", suggestion.fromName)
        assertEquals("Bruno", suggestion.toName)
        assertEquals(1000L, suggestion.amountCents)
    }

    @Test
    fun `marking a transaction as paid writes a Settlement and the suggestion list empties out`() = runTest(testDispatcher) {
        seedSimpleDebt()
        val viewModel = buildViewModel()
        val state = viewModel.uiState.first { it is SettleDebtsUiState.Content } as SettleDebtsUiState.Content
        val suggestion = state.suggestions.single()

        viewModel.onMarkAsPaidClick(suggestion)

        val stateAfterSettling = viewModel.uiState.first { it is SettleDebtsUiState.SettledUp }
        assertTrue(stateAfterSettling is SettleDebtsUiState.SettledUp)

        val settlements = settlementRepository.getSettlementsFlow(groupId).first()
        val settlement = settlements.single()
        assertEquals("a", settlement.payerId)
        assertEquals("b", settlement.receiverId)
        assertEquals(1000L, settlement.amount.cents)
        assertEquals(groupId, settlement.groupId)
    }
}
