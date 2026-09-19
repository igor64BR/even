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
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupSyncException
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
    ) = GroupDetailViewModel(
        groupId = groupId,
        groupRepository = groupRepository,
        participantRepository = participantRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        authRepository = authRepository,
        remoteGroupRepository = remoteGroupRepository,
        debtSimplificationEngine = GreedyDebtSimplificationEngine(),
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
    }
}
