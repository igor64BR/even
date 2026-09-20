package com.rateio.app.ui.groups

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.NotificationEntity
import com.rateio.data.persistence.entity.ParticipantEntity
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomNotificationRepository
import com.rateio.data.repository.RoomParticipantRepository
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
 * Smoke test de T8.2 ("dados de smoke test inseridos via Room diretamente num teste", já que a
 * tela "Novo grupo" — T16 — ainda não existia). Insere via os DAOs de verdade (mesmo Room de T7)
 * e confirma que [GroupListViewModel] combina [RoomGroupRepository] + [RoomParticipantRepository]
 * no [GroupListUiState] certo: sem grupos -> Empty; com grupo -> Content com a contagem de
 * participantes reativa. Mesmo padrão Robolectric do `RateioDatabaseTest` de `:data` (T7),
 * banco em memória, sem tocar disco nem precisar de emulador.
 *
 * T19.1/T19.2 tinha acrescentado cobertura de "Sincronizar este grupo" aqui, como atalho
 * temporário (não existia tela de detalhe de grupo ainda). T42.4 moveu essa ação e seus testes
 * para [com.rateio.app.ui.groupdetail.GroupDetailViewModelTest]. T41.2 acrescenta
 * `unreadNotificationsCount` (badge da aba "Avisos") — esta classe volta a cobrir só o que
 * [GroupListViewModel] de fato faz hoje, mais essa contagem.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GroupListViewModelTest {

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
    fun `badge de nao lidas reflete NotificationEntity com isRead false`() = runTest(testDispatcher) {
        database.groupDao().insert(
            GroupEntity(id = "churras", name = "Churras de sábado", createdAtEpochMillis = 1_000L),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "churras", message = "Marina lançou algo.", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "quitacao:s1", groupId = "churras", message = "Você quitou algo.", occurredAtEpochMillis = 2_000L, isRead = true),
        )

        val count = viewModel.unreadNotificationsCount.first { it == 1 }

        assertEquals(1, count)
    }
}
