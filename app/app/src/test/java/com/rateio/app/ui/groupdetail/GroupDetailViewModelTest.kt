package com.rateio.app.ui.groupdetail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.data.repository.RoomSettlementRepository
import com.rateio.domain.engine.GreedyDebtSimplificationEngine
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.realtime.GroupRealtimeGateway
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteExpenseRepository
import com.rateio.domain.repository.RemoteGroupRepository
import java.time.Instant
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cobre T42.2 (saldo exibido bate com `computeBalances`, T33, pra um grupo com despesas reais) e
 * a ação "Sincronizar este grupo" (T19, movida do card da lista pra cá em T42.4 — mesma cobertura
 * que existia em `GroupListViewModelTest` antes do rewire). Mesmo padrão Robolectric das demais
 * telas: banco Room em memória, DAOs de verdade por trás de `Room*Repository`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GroupDetailViewModelTest {

    private val authenticatedSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
    private lateinit var groupRepository: RoomGroupRepository
    private lateinit var participantRepository: RoomParticipantRepository
    private lateinit var expenseRepository: RoomExpenseRepository
    private lateinit var settlementRepository: RoomSettlementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RateioDatabase::class.java,
        ).build()
        groupRepository = RoomGroupRepository(database.groupDao())
        participantRepository = RoomParticipantRepository(database.participantDao())
        expenseRepository = RoomExpenseRepository(database.expenseDao())
        settlementRepository = RoomSettlementRepository(database.settlementDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        groupId: String,
        authRepository: AuthRepository = FakeAuthRepository(initialSession = null),
        remoteGroupRepository: RemoteGroupRepository = FakeRemoteGroupRepository(),
        remoteExpenseRepository: RemoteExpenseRepository = FakeRemoteExpenseRepository(),
        groupRealtimeGateway: GroupRealtimeGateway = FakeGroupRealtimeGateway(),
    ) = GroupDetailViewModel(
        groupId = groupId,
        groupRepository = groupRepository,
        participantRepository = participantRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        authRepository = authRepository,
        remoteGroupRepository = remoteGroupRepository,
        remoteExpenseRepository = remoteExpenseRepository,
        debtSimplificationEngine = GreedyDebtSimplificationEngine(),
        groupRealtimeGateway = groupRealtimeGateway,
    )

    private suspend fun seedGroupWithExpense(): String {
        val groupId = "churras"
        groupRepository.insertGroup(Group(id = groupId, name = "Churras de sábado", createdAt = Instant.now()))
        participantRepository.insertParticipant(Participant(id = "p1", groupId = groupId, name = "Você", isYou = true))
        participantRepository.insertParticipant(Participant(id = "p2", groupId = groupId, name = "Marina"))
        participantRepository.insertParticipant(Participant(id = "p3", groupId = groupId, name = "Diego"))
        expenseRepository.insertExpense(
            Expense(
                id = "e1",
                groupId = groupId,
                description = "Carvão e carne",
                amountCents = 1000,
                paidByParticipantId = "p1",
                createdAt = Instant.EPOCH,
                splits = listOf(
                    ExpenseSplit.Equal(participantId = "p1"),
                    ExpenseSplit.Equal(participantId = "p2"),
                    ExpenseSplit.Equal(participantId = "p3"),
                ),
            ),
        )
        return groupId
    }

    @Test
    fun `saldo exibido bate com computeBalances para um grupo com despesas reais`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState.first { it is GroupDetailUiState.Content } as GroupDetailUiState.Content

        // dividirIgualmente(1000, [p1,p2,p3]): base=333, resto=1 -> p1=334, p2=333, p3=333.
        // p1 pagou 1000 e deve 334 -> +666 (credor); p2/p3 devem 333 cada (devedores).
        assertEquals(3, state.balances.size)
        val p1 = state.balances.single { it.participantId == "p1" }
        val p2 = state.balances.single { it.participantId == "p2" }
        val p3 = state.balances.single { it.participantId == "p3" }
        assertEquals(ParticipantBalance.Credit(666), p1.balance)
        assertEquals(ParticipantBalance.Owed(333), p2.balance)
        assertEquals(ParticipantBalance.Owed(333), p3.balance)
        assertTrue("p1 é o participante 'você'", p1.isYou)

        assertEquals(1, state.expenses.size)
        val expenseRow = state.expenses.single()
        assertEquals("Carvão e carne", expenseRow.description)
        assertEquals("Você", expenseRow.payerName)
        assertEquals(1000L, expenseRow.amountCents)
        assertEquals("dividido igual", expenseRow.splitTypeLabel)
    }

    @Test
    fun `grupo com saldos zerados mostra todos os participantes quitados`() = runTest(testDispatcher) {
        val groupId = "republica"
        groupRepository.insertGroup(Group(id = groupId, name = "República", createdAt = Instant.now()))
        participantRepository.insertParticipant(Participant(id = "p1", groupId = groupId, name = "Você", isYou = true))
        participantRepository.insertParticipant(Participant(id = "p2", groupId = groupId, name = "Marina"))
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState.first { it is GroupDetailUiState.Content } as GroupDetailUiState.Content

        assertTrue(state.balances.all { it.balance is ParticipantBalance.Settled })
        assertTrue(state.expenses.isEmpty())
    }

    @Test
    fun `sincronizar com sucesso marca isSynced e esconde a acao`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(remoteIdFor = { "remote-$groupId" }),
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onSyncGroupClick()

        val stateAposSync = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.syncAction == GroupSyncActionUiState.Hidden }
                as GroupDetailUiState.Content
        assertTrue(stateAposSync.isSynced)

        val persisted = database.groupDao().getGroupById(groupId)!!
        assertTrue("sync bem-sucedido marca isSynced=true", persisted.isSynced)
        assertEquals("remote-$groupId", persisted.remoteId)
    }

    @Test
    fun `falha de rede na sincronizacao mantem isSynced false e mostra erro`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(
                failure = { GroupSyncException("Sem conexão com o servidor do Rateio.") },
            ),
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onSyncGroupClick()

        val stateAposFalha = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.syncAction is GroupSyncActionUiState.Failed }
                as GroupDetailUiState.Content
        val syncAction = stateAposFalha.syncAction as GroupSyncActionUiState.Failed
        assertEquals("Sem conexão com o servidor do Rateio.", syncAction.message)

        val persisted = database.groupDao().getGroupById(groupId)!!
        assertFalse("falha de rede nao pode deixar o grupo marcado como sincronizado", persisted.isSynced)
        assertNull(persisted.remoteId)
    }

    /**
     * Marca o grupo como sincronizado via `UPDATE` direto, nunca via `GroupDao.insert`
     * (`@Insert(OnConflictStrategy.REPLACE)`): re-inserir a MESMA linha (mesmo id) faz o SQLite
     * apagar e reinserir a linha, o que dispara `onDelete = CASCADE` das chaves estrangeiras de
     * `participants`/`expenses` pra `groups` — apagando silenciosamente os participantes/despesa
     * já semeados por [seedGroupWithExpense] (confirmado isolando o comportamento: contagem de
     * participantes vai a zero depois do REPLACE). **Bug real e pré-existente, fora do escopo de
     * T29** — a mesma chamada acontece em produção em [GroupDetailViewModel.syncGroup] (T19,
     * "Sincronizar este grupo"), reportado à parte, não corrigido aqui.
     */
    private fun markGroupAsSynced(groupId: String, remoteId: String) {
        database.openHelper.writableDatabase.execSQL(
            "UPDATE groups SET isSynced = 1, remoteId = ? WHERE id = ?",
            arrayOf(remoteId, groupId),
        )
    }

    // --- T29.2: excluir despesa ---

    @Test
    fun `excluir despesa local remove do Room e recalcula saldo`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(groupId)
        viewModel.uiState.first { it is GroupDetailUiState.Content && it.expenses.isNotEmpty() }

        viewModel.onDeleteExpenseClick("e1")

        val stateAposExcluir = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.expenses.isEmpty() } as GroupDetailUiState.Content
        assertTrue("sem despesas, todo mundo volta a ficar quitado", stateAposExcluir.balances.all { it.balance is ParticipantBalance.Settled })
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `excluir despesa de grupo nao sincronizado nao chama o backend`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = buildViewModel(groupId, remoteExpenseRepository = remoteExpenseRepository)
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onDeleteExpenseClick("e1")
        viewModel.uiState.first { it is GroupDetailUiState.Content && it.expenses.isEmpty() }

        assertNull("grupo local (nao sincronizado) nunca deve tentar falar com o backend", remoteExpenseRepository.lastDeletedExpenseId)
    }

    @Test
    fun `excluir despesa de grupo sincronizado propaga a exclusao pro backend`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        markGroupAsSynced(groupId, remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = buildViewModel(groupId, remoteExpenseRepository = remoteExpenseRepository)
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onDeleteExpenseClick("e1")
        val (remoteGroupId, deletedExpenseId) = remoteExpenseRepository.awaitDelete()

        assertEquals("remote-$groupId", remoteGroupId)
        assertEquals("e1", deletedExpenseId)
    }

    @Test
    fun `falha de rede ao propagar exclusao nao desfaz a remocao local`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        markGroupAsSynced(groupId, remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository(
            failure = { GroupSyncException("Sem conexão com o servidor do Rateio.") },
        )
        val viewModel = buildViewModel(groupId, remoteExpenseRepository = remoteExpenseRepository)
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onDeleteExpenseClick("e1")
        remoteExpenseRepository.awaitDelete() // espera a tentativa de propagação (que vai falhar) acontecer.

        val stateAposExcluir = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.expenses.isEmpty() } as GroupDetailUiState.Content
        assertTrue("exclusao local vale independente da falha de rede (local-first)", stateAposExcluir.expenses.isEmpty())
    }

    // T40.1: GroupDetailViewModel.startRealtimeUpdates()/stopRealtimeUpdates() só orquestram QUANDO
    // conectar/desconectar — a lógica de conexão em si (SignalRGroupRealtimeGateway) é testada à
    // parte em :data, sem HubConnection nenhum (ver GroupEventRecorderTest/
    // MissedGroupEventsSynchronizerTest). Aqui só verificamos que o gateway é chamado com os ids
    // certos, nas condições certas.

    @Test
    fun `startRealtimeUpdates conecta o gateway com localGroupId e remoteId quando grupo sincronizado e autenticado`() =
        runTest(testDispatcher) {
            val groupId = seedGroupWithExpense()
            database.groupDao().insert(
                database.groupDao().getGroupById(groupId)!!.copy(isSynced = true, remoteId = "remote-$groupId"),
            )
            val gateway = FakeGroupRealtimeGateway()
            val viewModel = buildViewModel(
                groupId = groupId,
                authRepository = FakeAuthRepository(authenticatedSession),
                groupRealtimeGateway = gateway,
            )
            viewModel.uiState.first { it is GroupDetailUiState.Content }

            viewModel.startRealtimeUpdates()
            val firstConnect = gateway.awaitConnect()

            assertEquals(groupId to "remote-$groupId", firstConnect)
            assertEquals(0, gateway.disconnectCallCount)
        }

    @Test
    fun `startRealtimeUpdates nao conecta o gateway quando grupo nao esta sincronizado`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val gateway = FakeGroupRealtimeGateway()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            groupRealtimeGateway = gateway,
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.startRealtimeUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("grupo nao sincronizado nao pode abrir conexao (nao tem remoteId)", gateway.connectCalls.isEmpty())
    }

    @Test
    fun `startRealtimeUpdates nao conecta o gateway quando nao ha sessao autenticada`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        database.groupDao().insert(
            database.groupDao().getGroupById(groupId)!!.copy(isSynced = true, remoteId = "remote-$groupId"),
        )
        val gateway = FakeGroupRealtimeGateway()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(initialSession = null),
            groupRealtimeGateway = gateway,
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.startRealtimeUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("sem sessao nao pode abrir conexao", gateway.connectCalls.isEmpty())
    }

    @Test
    fun `stopRealtimeUpdates desconecta uma conexao ativa`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        database.groupDao().insert(
            database.groupDao().getGroupById(groupId)!!.copy(isSynced = true, remoteId = "remote-$groupId"),
        )
        val gateway = FakeGroupRealtimeGateway()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            groupRealtimeGateway = gateway,
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }
        viewModel.startRealtimeUpdates()
        gateway.awaitConnect()

        viewModel.stopRealtimeUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, gateway.disconnectCallCount)
    }

    /**
     * [awaitConnect] existe porque [com.rateio.domain.repository.GroupRepository.getGroupsFlow]
     * (Room) entrega sua primeira emissão a partir de um executor real, fora do controle do
     * `TestCoroutineScheduler` — `testDispatcher.scheduler.advanceUntilIdle()` não espera por isso
     * de forma confiável (corrida: nada garante que a query do Room já terminou quando
     * `advanceUntilIdle()` roda). Suspender de verdade em cima de um `Channel` é a forma
     * determinística de esperar o primeiro `connect()` acontecer.
     */
    private class FakeGroupRealtimeGateway : GroupRealtimeGateway {
        val connectCalls = mutableListOf<Pair<String, String>>()
        var disconnectCallCount = 0
            private set
        private val connectSignal = kotlinx.coroutines.channels.Channel<Pair<String, String>>(
            kotlinx.coroutines.channels.Channel.UNLIMITED,
        )

        override suspend fun connect(localGroupId: String, remoteGroupId: String) {
            val call = localGroupId to remoteGroupId
            connectCalls += call
            connectSignal.send(call)
        }

        override suspend fun disconnect() {
            disconnectCallCount += 1
        }

        suspend fun awaitConnect(): Pair<String, String> = connectSignal.receive()
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

    private class FakeRemoteGroupRepository(
        private val remoteIdFor: (Group) -> String = { "remote-${it.id}" },
        private val failure: (() -> Throwable)? = null,
    ) : RemoteGroupRepository {
        override suspend fun syncGroup(group: Group, participants: List<Participant>, expenses: List<Expense>): String {
            failure?.invoke()?.let { throw it }
            return remoteIdFor(group)
        }

        override suspend fun joinByCode(inviteCode: String): String {
            throw UnsupportedOperationException("não usado neste teste — ver JoinGroupViewModelTest (T22)")
        }
    }

    /**
     * [awaitDelete] existe pela mesma razão de [FakeGroupRealtimeGateway.awaitConnect]:
     * [GroupDetailViewModel.onDeleteExpenseClick] dispara num `viewModelScope.launch` próprio, e o
     * `Flow` de despesas (Room) já pode ter emitido "lista vazia" — o sinal que os testes usam pra
     * saber que a exclusão local aconteceu — antes da chamada de propagação pro backend, mais
     * adiante na mesma coroutine, ter rodado. Suspender de verdade num `Channel` é a forma
     * determinística de esperar a tentativa de propagação (sucesso ou falha) acontecer.
     */
    private class FakeRemoteExpenseRepository(
        private val failure: (() -> Throwable)? = null,
    ) : RemoteExpenseRepository {
        var lastRemoteGroupId: String? = null
            private set
        var lastDeletedExpenseId: String? = null
            private set
        private val deleteSignal = kotlinx.coroutines.channels.Channel<Pair<String, String>>(
            kotlinx.coroutines.channels.Channel.UNLIMITED,
        )

        override suspend fun updateExpense(remoteGroupId: String, expense: Expense) {
            throw UnsupportedOperationException("não usado neste teste — ver CreateExpenseViewModelTest (T29.1)")
        }

        override suspend fun deleteExpense(remoteGroupId: String, expenseId: String) {
            lastRemoteGroupId = remoteGroupId
            lastDeletedExpenseId = expenseId
            deleteSignal.send(remoteGroupId to expenseId)
            failure?.invoke()?.let { throw it }
        }

        suspend fun awaitDelete(): Pair<String, String> = deleteSignal.receive()
    }
}
