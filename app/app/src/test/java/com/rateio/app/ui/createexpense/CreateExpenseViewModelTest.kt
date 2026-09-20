package com.rateio.app.ui.createexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cobre T24.2 (cálculo ao vivo da divisão igual) e T24.3/T24.4 (persistência local + validação)
 * do formulário "Nova despesa". Mesmo padrão Robolectric de `CreateGroupViewModelTest` (T16):
 * banco Room em memória, DAOs de verdade por trás de `Room*Repository`, grupo + participantes
 * semeados diretamente via repositório (não existe tela de detalhe de grupo pra semear via UI).
 *
 * O carregamento inicial de participantes ([CreateExpenseViewModel.init]) e a gravação da despesa
 * ([CreateExpenseViewModel.onSaveClick]) rodam no `TransactionExecutor` de verdade do Room, fora
 * do [StandardTestDispatcher] — `advanceUntilIdle()` sozinho não espera esse trabalho real
 * terminar (mesma corrida documentada em `GroupListViewModelTest`, "sincronizar com sucesso").
 * Por isso os testes esperam suspendendo de verdade em `Flow.first { predicado }` (sobre
 * [CreateExpenseViewModel.uiState]/[CreateExpenseViewModel.events]/o repositório), nunca só lendo
 * `.value` depois de `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
    private lateinit var groupRepository: RoomGroupRepository
    private lateinit var participantRepository: RoomParticipantRepository
    private lateinit var expenseRepository: RoomExpenseRepository
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

        groupId = "grupo-1"
        groupRepository.insertGroup(Group(id = groupId, name = "Churras de sábado", createdAt = Instant.now()))
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun seedParticipants(vararg participants: Participant) {
        participants.forEach { participantRepository.insertParticipant(it) }
    }

    /** Cria o ViewModel e suspende de verdade até o carregamento inicial de participantes chegar. */
    private suspend fun createViewModelWithParticipantsLoaded(): CreateExpenseViewModel {
        val viewModel = CreateExpenseViewModel(
            groupId = groupId,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
        )
        viewModel.uiState.first { it.participants.isNotEmpty() }
        return viewModel
    }

    @Test
    fun `divisao igual entre 3 participantes fecha o total exato mesmo sem divisao inteira`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onAmountChanged("10,00")

        val rows = viewModel.uiState.value.splitRows
        assertEquals(3, rows.size)
        assertEquals(1000L, rows.sumOf { it.amountCents })
        // "p1" é o primeiro em ordem de participantId, recebe o centavo extra (334+333+333=1000).
        assertEquals(334L, rows.single { it.participantId == "p1" }.amountCents)
        assertEquals(333L, rows.single { it.participantId == "p2" }.amountCents)
        assertEquals(333L, rows.single { it.participantId == "p3" }.amountCents)
    }

    @Test
    fun `desmarcar um participante recalcula a divisao so entre os restantes`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
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
    fun `salvar sem descricao marca erro e nao persiste nada`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "Você", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onAmountChanged("10,00")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.descriptionError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `salvar com valor zero marca erro e nao persiste nada`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "Você", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Jantar de sexta")
        viewModel.onAmountChanged("0")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.amountError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `salvar sem nenhum participante selecionado marca erro e nao persiste nada`() = runTest(testDispatcher) {
        seedParticipants(Participant(id = "p1", groupId = groupId, name = "Você", isYou = true))
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Jantar de sexta")
        viewModel.onAmountChanged("10,00")
        viewModel.onParticipantToggled("p1")
        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.participantsError)
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `salvar despesa valida persiste no Room com splits Equal e emite evento`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onDescriptionChanged("Jantar de sexta")
        viewModel.onAmountChanged("10,00")
        viewModel.onPayerSelected("p2")
        viewModel.onSaveClick()

        // Suspensão real: só retorna depois que a despesa foi persistida e o evento emitido
        // (ver nota de classe) — nada de advanceUntilIdle() sozinho aqui.
        viewModel.events.first()

        val expenses = expenseRepository.getExpensesFlow(groupId).first()
        assertEquals(1, expenses.size)
        val expense = expenses.single()
        assertEquals("Jantar de sexta", expense.description)
        assertEquals(1000L, expense.amountCents)
        assertEquals("p2", expense.paidByParticipantId)
        assertEquals(3, expense.splits.size)
        assertTrue(expense.splits.all { it is ExpenseSplit.Equal })
        assertEquals(setOf("p1", "p2", "p3"), expense.splits.map { it.participantId }.toSet())
    }

    // --- T26.1: aba Percentual ---

    @Test
    fun `salvar com percentual que nao fecha 100 por cento nao persiste nada`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.PERCENTAGE)
        viewModel.onDescriptionChanged("Jantar de sexta")
        viewModel.onAmountChanged("100,00")
        viewModel.onPercentageChanged("p1", "40")
        viewModel.onPercentageChanged("p2", "40")
        viewModel.onSaveClick()

        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `salvar despesa com divisao percentual persiste splits Weight correspondentes`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.PERCENTAGE)
        viewModel.onDescriptionChanged("Jantar de sexta")
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

    // --- T26.2: aba Valor fixo ---

    @Test
    fun `salvar com valor fixo que nao fecha o total nao persiste nada`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.FIXED_AMOUNT)
        viewModel.onDescriptionChanged("Jantar de sexta")
        viewModel.onAmountChanged("100,00")
        viewModel.onFixedAmountChanged("p1", "40,00")
        viewModel.onFixedAmountChanged("p2", "40,00")
        viewModel.onSaveClick()

        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `salvar despesa com divisao por valor fixo persiste splits FixedAmount correspondentes`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val viewModel = createViewModelWithParticipantsLoaded()

        viewModel.onSplitModeSelected(SplitMode.FIXED_AMOUNT)
        viewModel.onDescriptionChanged("Jantar de sexta")
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
}
