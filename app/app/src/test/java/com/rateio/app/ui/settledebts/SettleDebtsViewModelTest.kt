package com.rateio.app.ui.settledebts

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.data.repository.RoomSettlementRepository
import com.rateio.domain.engine.GreedyDebtSimplificationEngine
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
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
 * Cobre T42.3: `computeSettlement` (T33) sobre os saldos atuais vira a lista de sugestões;
 * "Marcar como pago" grava um `Settlement` (T42.1) e a lista recalcula sozinha (é derivada do
 * Flow de quitações, não um estado cacheado); grupo com saldos zerados entra direto em
 * [SettleDebtsUiState.SettledUp]. Mesmo padrão Robolectric das demais telas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettleDebtsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
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
            RateioDatabase::class.java,
        ).build()
        groupRepository = RoomGroupRepository(database.groupDao())
        participantRepository = RoomParticipantRepository(database.participantDao())
        expenseRepository = RoomExpenseRepository(database.expenseDao())
        settlementRepository = RoomSettlementRepository(database.settlementDao())

        groupId = "churras"
        groupRepository.insertGroup(Group(id = groupId, name = "Churras de sábado", createdAt = Instant.now()))
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

    /** A: -1000 (deve) · B: +1000 (recebe) — `case-01-simples` de algorithm-spec.md. */
    private suspend fun seedSimpleDebt() {
        participantRepository.insertParticipant(Participant(id = "a", groupId = groupId, name = "Ana"))
        participantRepository.insertParticipant(Participant(id = "b", groupId = groupId, name = "Bruno"))
        expenseRepository.insertExpense(
            Expense(
                id = "e1",
                groupId = groupId,
                description = "Cerveja",
                amountCents = 2000,
                paidByParticipantId = "b",
                createdAt = Instant.EPOCH,
                splits = listOf(ExpenseSplit.Equal(participantId = "a"), ExpenseSplit.Equal(participantId = "b")),
            ),
        )
    }

    @Test
    fun `grupo com saldos zerados mostra estado quitado`() = runTest(testDispatcher) {
        participantRepository.insertParticipant(Participant(id = "a", groupId = groupId, name = "Ana"))
        val viewModel = buildViewModel()

        val state = viewModel.uiState.first { it !is SettleDebtsUiState.Loading }

        assertTrue(state is SettleDebtsUiState.SettledUp)
        assertEquals("Churras de sábado", (state as SettleDebtsUiState.SettledUp).groupName)
    }

    @Test
    fun `sugestao de quitacao usa computeSettlement sobre os saldos atuais`() = runTest(testDispatcher) {
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
    fun `marcar transacao como paga grava Settlement e a lista de sugestoes esvazia`() = runTest(testDispatcher) {
        seedSimpleDebt()
        val viewModel = buildViewModel()
        val state = viewModel.uiState.first { it is SettleDebtsUiState.Content } as SettleDebtsUiState.Content
        val suggestion = state.suggestions.single()

        viewModel.onMarkAsPaidClick(suggestion)

        val stateAposQuitar = viewModel.uiState.first { it is SettleDebtsUiState.SettledUp }
        assertTrue(stateAposQuitar is SettleDebtsUiState.SettledUp)

        val settlements = settlementRepository.getSettlementsFlow(groupId).first()
        val settlement = settlements.single()
        assertEquals("a", settlement.payerId)
        assertEquals("b", settlement.receiverId)
        assertEquals(1000L, settlement.amount.cents)
        assertEquals(groupId, settlement.groupId)
    }
}
