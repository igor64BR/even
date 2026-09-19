package com.rateio.app.ui.groups

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.ParticipantEntity
import com.rateio.data.repository.RoomExpenseRepository
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.model.Expense
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteGroupRepository
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
 * Smoke test de T8.2 ("dados de smoke test inseridos via Room diretamente num teste", já que a
 * tela "Novo grupo" — T16 — ainda não existe). Insere via os DAOs de verdade (mesmo Room de T7)
 * e confirma que [GroupListViewModel] combina [RoomGroupRepository] + [RoomParticipantRepository]
 * no [GroupListUiState] certo: sem grupos -> Empty; com grupo -> Content com a contagem de
 * participantes reativa. Mesmo padrão Robolectric do `RateioDatabaseTest` de `:data` (T7),
 * banco em memória, sem tocar disco nem precisar de emulador.
 *
 * T19.1/T19.2 acrescenta a cobertura de `onSyncGroupClick`: sucesso marca `isSynced=true` +
 * `remoteId` no Room de verdade; falha de rede ([GroupSyncException], dublê de
 * [RemoteGroupRepository]) mantém `isSynced=false` — nenhum estado inconsistente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GroupListViewModelTest {

    private val authenticatedSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
    private lateinit var viewModel: GroupListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RateioDatabase::class.java,
        ).build()
        viewModel = buildViewModel(authRepository = FakeAuthRepository(initialSession = null))
    }

    private fun buildViewModel(
        authRepository: AuthRepository,
        remoteGroupRepository: RemoteGroupRepository = FakeRemoteGroupRepository(),
    ) = GroupListViewModel(
        groupRepository = RoomGroupRepository(database.groupDao()),
        participantRepository = RoomParticipantRepository(database.participantDao()),
        expenseRepository = RoomExpenseRepository(database.expenseDao()),
        authRepository = authRepository,
        remoteGroupRepository = remoteGroupRepository,
    )

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `sem grupos no Room, estado e Empty`() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { it !is GroupListUiState.Loading }

        assertEquals(GroupListUiState.Empty, state)
    }

    @Test
    fun `grupo com participantes no Room aparece no Content com a contagem certa`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "churras", name = "Churras de sábado", createdAtEpochMillis = 1_000L),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "voce", groupId = "churras", name = "Você", isYou = true),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "marina", groupId = "churras", name = "Marina", isYou = false),
        )

        val state = viewModel.uiState.first { it is GroupListUiState.Content } as GroupListUiState.Content

        val group = state.groups.single()
        assertEquals("Churras de sábado", group.name)
        assertEquals(2, group.participantCount)
        assertEquals("CH", group.tag)
        assertTrue(group.balance is GroupBalance.Settled)
        assertTrue("grupo inserido sem isSynced explícito nasce local (isSynced=false)", !group.isSynced)
        assertEquals(
            "sem sessão autenticada, a ação de sincronizar fica escondida (T19)",
            GroupSyncActionUiState.Hidden,
            group.syncAction,
        )
    }

    @Test
    fun `grupo sincronizado no Room aparece com isSynced true no Content`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "viagem", name = "Viagem", createdAtEpochMillis = 2_000L, isSynced = true),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "voce", groupId = "viagem", name = "Você", isYou = true),
        )

        val state = viewModel.uiState.first { it is GroupListUiState.Content } as GroupListUiState.Content

        val group = state.groups.single()
        assertTrue(
            "GroupListViewModel.toUiModel() precisa repassar Group.isSynced real (T7B), não mais o false fixo de T8",
            group.isSynced,
        )
    }

    @Test
    fun `usuario autenticado com grupo local ve acao Available`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "churras", name = "Churras de sábado", createdAtEpochMillis = 1_000L),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "voce", groupId = "churras", name = "Você", isYou = true),
        )
        val viewModelAutenticado = buildViewModel(authRepository = FakeAuthRepository(authenticatedSession))

        val state = viewModelAutenticado.uiState.first { it is GroupListUiState.Content } as GroupListUiState.Content

        assertEquals(GroupSyncActionUiState.Available, state.groups.single().syncAction)
    }

    @Test
    fun `sincronizar com sucesso marca isSynced e salva remoteId no Room`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "churras", name = "Churras de sábado", createdAtEpochMillis = 1_000L),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "voce", groupId = "churras", name = "Você", isYou = true),
        )
        val viewModelAutenticado = buildViewModel(
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(remoteIdFor = { "remote-churras" }),
        )
        viewModelAutenticado.uiState.first { it is GroupListUiState.Content }

        viewModelAutenticado.onSyncGroupClick("churras")
        // Room roda a query/gravação de verdade num thread pool próprio (TransactionExecutor),
        // fora do StandardTestDispatcher -- advanceUntilIdle() sozinho não espera esse trabalho
        // real terminar (corrida observada neste teste). Esperar o próprio uiState refletir
        // syncAction=Hidden é suspensão real: só acontece depois que insertGroup(isSynced=true)
        // gravou no Room E groupRepository.getGroupsFlow() reemitiu, então quando o first{}
        // retorna a gravação já está garantidamente commitada.
        val stateAposSync = viewModelAutenticado.uiState
            .first { it is GroupListUiState.Content && it.groups.single().syncAction == GroupSyncActionUiState.Hidden }
                as GroupListUiState.Content
        assertEquals(GroupSyncActionUiState.Hidden, stateAposSync.groups.single().syncAction)

        val persisted = database.groupDao().getGroupById("churras")!!
        assertTrue("sync bem-sucedido marca isSynced=true", persisted.isSynced)
        assertEquals("remote-churras", persisted.remoteId)
    }

    @Test
    fun `falha de rede na sincronizacao mantem isSynced false e mostra erro`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "churras", name = "Churras de sábado", createdAtEpochMillis = 1_000L),
        )
        database.participantDao().insert(
            ParticipantEntity(id = "voce", groupId = "churras", name = "Você", isYou = true),
        )
        val viewModelAutenticado = buildViewModel(
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(
                failure = { GroupSyncException("Sem conexão com o servidor do Rateio.") },
            ),
        )
        viewModelAutenticado.uiState.first { it is GroupListUiState.Content }

        viewModelAutenticado.onSyncGroupClick("churras")
        // Mesma corrida do teste de sucesso (ver comentário lá): esperar o uiState refletir
        // Failed é suspensão real, não depende de advanceUntilIdle() adivinhar quando o Room
        // (thread pool próprio) terminou de reemitir a lista.
        val stateAposFalha = viewModelAutenticado.uiState
            .first { it is GroupListUiState.Content && it.groups.single().syncAction is GroupSyncActionUiState.Failed }
                as GroupListUiState.Content
        val syncAction = stateAposFalha.groups.single().syncAction as GroupSyncActionUiState.Failed
        assertEquals("Sem conexão com o servidor do Rateio.", syncAction.message)

        val persisted = database.groupDao().getGroupById("churras")!!
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
