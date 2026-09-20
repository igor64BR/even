package com.rateio.app.ui.createexpense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteExpenseRepository
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

    /**
     * Cria o ViewModel e suspende de verdade até o carregamento inicial de participantes chegar.
     * [expenseId] (T29) liga o modo edição — `null` (default) mantém o comportamento de sempre
     * (T24, modo criação). [remoteExpenseRepository] só importa pros testes de propagação da
     * edição pro backend (grupo sincronizado); os demais usam o fake padrão, que nunca é chamado
     * porque nenhum grupo semeado aqui tem `isSynced = true`.
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

    // --- T29.1: modo edição ---

    private suspend fun seedExpense(): Expense {
        val expense = Expense(
            id = "e1",
            groupId = groupId,
            description = "Carvão e carne",
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
    fun `abrir em modo edicao pre-preenche descricao valor pagador e divisao da despesa existente`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
            Participant(id = "p3", groupId = groupId, name = "Diego"),
        )
        val expense = seedExpense()

        val viewModel = createViewModelWithParticipantsLoaded(expenseId = expense.id)
        val state = viewModel.uiState.first { it.description == "Carvão e carne" }

        assertTrue("uiState.expenseId deve apontar pra despesa sendo editada", state.isEditMode)
        assertEquals("10,00", state.amountInput)
        assertEquals("p1", state.payerId)
        assertEquals(SplitMode.EQUAL, state.splitMode)
        assertTrue(state.splitRows.single { it.participantId == "p1" }.isIncluded)
        assertTrue(state.splitRows.single { it.participantId == "p2" }.isIncluded)
        assertFalse(
            "p3 nao estava na despesa original, nao deve entrar marcado ao reabrir pra editar",
            state.splitRows.single { it.participantId == "p3" }.isIncluded,
        )
    }

    @Test
    fun `salvar edicao substitui a despesa existente no Room mantendo o mesmo id`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        val viewModel = createViewModelWithParticipantsLoaded(expenseId = expense.id)
        viewModel.uiState.first { it.description == "Carvão e carne" }

        viewModel.onDescriptionChanged("Carvão, carne e gelo")
        viewModel.onAmountChanged("20,00")
        viewModel.onSaveClick()
        viewModel.events.first()

        val expenses = expenseRepository.getExpensesFlow(groupId).first()
        assertEquals("edicao substitui a despesa (upsert), nunca duplica", 1, expenses.size)
        val updated = expenses.single()
        assertEquals("e1", updated.id)
        assertEquals("Carvão, carne e gelo", updated.description)
        assertEquals(2000L, updated.amountCents)
    }

    @Test
    fun `salvar edicao de grupo nao sincronizado nao chama o backend`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Carvão e carne" }

        viewModel.onDescriptionChanged("Carvão, carne e gelo")
        viewModel.onSaveClick()
        viewModel.events.first()

        assertNull("grupo local (nao sincronizado) nunca deve tentar falar com o backend", remoteExpenseRepository.lastUpdatedExpense)
    }

    /**
     * Marca o grupo como sincronizado sem passar por `GroupRepository.insertGroup`/
     * `@Insert(OnConflictStrategy.REPLACE)`: re-inserir a MESMA linha (mesmo id) via REPLACE faz o
     * SQLite apagar e reinserir a linha, o que dispara `onDelete = CASCADE` das chaves estrangeiras
     * de `participants`/`expenses` pra `groups` — apagando os participantes/despesa que acabamos
     * de semear (confirmado isolando o comportamento: contagem de participantes vai de 2 pra 0
     * depois do REPLACE). **Esse é um bug real e pré-existente fora do escopo de T29** — a mesma
     * chamada acontece em produção em `GroupDetailViewModel.syncGroup` (T19, "Sincronizar este
     * grupo"), então sincronizar um grupo que já tem participantes/despesas locais hoje os apaga
     * silenciosamente; reportado à parte, não corrigido aqui. Pra não reproduzir esse bug só como
     * armadilha de teste, este helper faz um `UPDATE` direto (não passa por REPLACE nenhum).
     */
    private fun markGroupAsSynced(remoteId: String) {
        database.openHelper.writableDatabase.execSQL(
            "UPDATE groups SET isSynced = 1, remoteId = ? WHERE id = ?",
            arrayOf(remoteId, groupId),
        )
    }

    @Test
    fun `salvar edicao de grupo sincronizado propaga a mudanca pro backend`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        markGroupAsSynced(remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Carvão e carne" }

        viewModel.onDescriptionChanged("Carvão, carne e gelo")
        viewModel.onSaveClick()
        viewModel.events.first()

        assertEquals("remote-$groupId", remoteExpenseRepository.lastRemoteGroupId)
        assertEquals("Carvão, carne e gelo", remoteExpenseRepository.lastUpdatedExpense?.description)
    }

    @Test
    fun `falha de rede ao propagar edicao nao desfaz a mudanca local nem trava o evento de salvo`() = runTest(testDispatcher) {
        seedParticipants(
            Participant(id = "p1", groupId = groupId, name = "Você", isYou = true),
            Participant(id = "p2", groupId = groupId, name = "Marina"),
        )
        val expense = seedExpense()
        markGroupAsSynced(remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository(
            failure = { GroupSyncException("Sem conexão com o servidor do Rateio.") },
        )
        val viewModel = createViewModelWithParticipantsLoaded(
            expenseId = expense.id,
            remoteExpenseRepository = remoteExpenseRepository,
        )
        viewModel.uiState.first { it.description == "Carvão e carne" }

        viewModel.onDescriptionChanged("Carvão, carne e gelo")
        viewModel.onSaveClick()

        // Suspensão real: o evento chega mesmo com a propagação remota falhando.
        viewModel.events.first()

        val updated = expenseRepository.getExpensesFlow(groupId).first().single()
        assertEquals("mudanca local vale independente da falha de rede (local-first)", "Carvão, carne e gelo", updated.description)
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
            throw UnsupportedOperationException("não usado neste teste")
        }
    }
}
