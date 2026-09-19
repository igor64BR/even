package com.rateio.app.ui.groups

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.ParticipantEntity
import com.rateio.data.repository.RoomGroupRepository
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
 * tela "Novo grupo" — T16 — ainda não existe). Insere via os DAOs de verdade (mesmo Room de T7)
 * e confirma que [GroupListViewModel] combina [RoomGroupRepository] + [RoomParticipantRepository]
 * no [GroupListUiState] certo: sem grupos -> Empty; com grupo -> Content com a contagem de
 * participantes reativa. Mesmo padrão Robolectric do `RateioDatabaseTest` de `:data` (T7),
 * banco em memória, sem tocar disco nem precisar de emulador.
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
}
