package com.rateio.app.ui.creategroup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 * Cobre T16.2 (validação client-side) e T16.3 (persistência local via Room). Mesmo padrão
 * Robolectric de `GroupListViewModelTest` (T8): banco Room em memória, sem emulador, DAOs de
 * verdade por trás de [RoomGroupRepository]/[RoomParticipantRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreateGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: RateioDatabase
    private lateinit var viewModel: CreateGroupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RateioDatabase::class.java,
        ).build()
        viewModel = CreateGroupViewModel(
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
    fun `formulario nasce com Voce fixo e nao removivel`() {
        val participants = viewModel.uiState.value.participants

        assertEquals(1, participants.size)
        assertTrue(participants.single().isYou)
    }

    @Test
    fun `salvar sem nome marca erro e nao persiste nada`() = runTest(testDispatcher) {
        viewModel.onNewParticipantNameChanged("Marina")
        viewModel.onAddParticipant()

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.nameError)
        assertTrue(database.groupDao().getGroupsFlow().first().isEmpty())
    }

    @Test
    fun `salvar com menos de 2 participantes marca erro e nao persiste nada`() = runTest(testDispatcher) {
        viewModel.onNameChanged("Churras de sábado")

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.participantsError)
        assertTrue(database.groupDao().getGroupsFlow().first().isEmpty())
    }

    @Test
    fun `remover Voce nao tem efeito`() {
        val youId = viewModel.uiState.value.participants.single().id

        viewModel.onRemoveParticipant(youId)

        assertEquals(1, viewModel.uiState.value.participants.size)
    }

    @Test
    fun `salvar grupo valido persiste grupo e participantes no Room e emite evento`() = runTest(testDispatcher) {
        var eventEmitted = false
        val collectorJob = backgroundScope.launch(testDispatcher) {
            viewModel.events.collect { eventEmitted = true }
        }

        viewModel.onNameChanged("Churras de sábado")
        viewModel.onCategorySelected(GroupCategory.CHURRASCO)
        viewModel.onNewParticipantNameChanged("Marina")
        viewModel.onAddParticipant()

        viewModel.onSaveClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val groups = database.groupDao().getGroupsFlow().first()
        assertEquals(1, groups.size)
        assertEquals("Churras de sábado", groups.single().name)
        assertFalse("grupo criado localmente nasce nao sincronizado", groups.single().isSynced)

        val participants = database.participantDao().getParticipantsFlow(groups.single().id).first()
        assertEquals(2, participants.size)
        assertTrue(participants.any { it.name == "Você" && it.isYou })
        assertTrue(participants.any { it.name == "Marina" && !it.isYou })

        assertTrue("CreateGroupEvent.GroupCreated deveria ter sido emitido", eventEmitted)
        collectorJob.cancel()
    }
}
