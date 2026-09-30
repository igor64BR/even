package com.tally.app.ui.groupdetail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tally.data.persistence.TallyDatabase
import com.tally.data.repository.RoomExpenseRepository
import com.tally.data.repository.RoomGroupRepository
import com.tally.data.repository.RoomParticipantRepository
import com.tally.data.repository.RoomSettlementRepository
import com.tally.domain.engine.GreedyDebtSimplificationEngine
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser
import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Group
import com.tally.domain.model.Money
import com.tally.domain.model.Participant
import com.tally.domain.model.Settlement
import com.tally.domain.realtime.GroupRealtimeGateway
import com.tally.domain.repository.AuthRepository
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.RemoteExpenseRepository
import com.tally.domain.repository.RemoteGroupRepository
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
 * Covers the displayed balance matching `computeBalances` for a group with real expenses, and the
 * "Sync this group" action (the same coverage that existed in `GroupListViewModelTest` before the
 * rewire). Same Robolectric pattern as the other screens: an in-memory Room database, real DAOs
 * behind `Room*Repository`.
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
    private lateinit var database: TallyDatabase
    private lateinit var groupRepository: RoomGroupRepository
    private lateinit var participantRepository: RoomParticipantRepository
    private lateinit var expenseRepository: RoomExpenseRepository
    private lateinit var settlementRepository: RoomSettlementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TallyDatabase::class.java,
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
        val groupId = "barbecue"
        groupRepository.insertGroup(Group(id = groupId, name = "Saturday barbecue", createdAt = Instant.now()))
        participantRepository.insertParticipant(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        participantRepository.insertParticipant(Participant(id = "p2", groupId = groupId, name = "Marina"))
        participantRepository.insertParticipant(Participant(id = "p3", groupId = groupId, name = "Diego"))
        expenseRepository.insertExpense(
            Expense(
                id = "e1",
                groupId = groupId,
                description = "Charcoal and meat",
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
    fun `the displayed balance matches computeBalances for a group with real expenses`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState.first { it is GroupDetailUiState.Content } as GroupDetailUiState.Content

        // splitEqually(1000, [p1,p2,p3]): base=333, remainder=1 -> p1=334, p2=333, p3=333.
        // p1 paid 1000 and owes 334 -> +666 (creditor); p2/p3 owe 333 each (debtors).
        assertEquals(3, state.balances.size)
        val p1 = state.balances.single { it.participantId == "p1" }
        val p2 = state.balances.single { it.participantId == "p2" }
        val p3 = state.balances.single { it.participantId == "p3" }
        assertEquals(ParticipantBalance.Credit(666), p1.balance)
        assertEquals(ParticipantBalance.Owed(333), p2.balance)
        assertEquals(ParticipantBalance.Owed(333), p3.balance)
        assertTrue("p1 is the 'you' participant", p1.isYou)

        assertEquals(1, state.expenses.size)
        val expenseRow = state.expenses.single()
        assertEquals("Charcoal and meat", expenseRow.description)
        assertEquals("You", expenseRow.payerName)
        assertEquals(1000L, expenseRow.amountCents)
        assertEquals("split equally", expenseRow.splitTypeLabel)
    }

    @Test
    fun `a group with zeroed balances shows every participant as settled`() = runTest(testDispatcher) {
        val groupId = "household"
        groupRepository.insertGroup(Group(id = groupId, name = "Household", createdAt = Instant.now()))
        participantRepository.insertParticipant(Participant(id = "p1", groupId = groupId, name = "You", isYou = true))
        participantRepository.insertParticipant(Participant(id = "p2", groupId = groupId, name = "Marina"))
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState.first { it is GroupDetailUiState.Content } as GroupDetailUiState.Content

        assertTrue(state.balances.all { it.balance is ParticipantBalance.Settled })
        assertTrue(state.expenses.isEmpty())
    }

    @Test
    fun `syncing successfully marks isSynced and hides the action`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(remoteIdFor = { "remote-$groupId" }),
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onSyncGroupClick()

        val stateAfterSync = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.syncAction == GroupSyncActionUiState.Hidden }
                as GroupDetailUiState.Content
        assertTrue(stateAfterSync.isSynced)

        val persisted = database.groupDao().getGroupById(groupId)!!
        assertTrue("a successful sync marks isSynced=true", persisted.isSynced)
        assertEquals("remote-$groupId", persisted.remoteId)
    }

    @Test
    fun `a network failure while syncing keeps isSynced false and shows an error`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(
            groupId = groupId,
            authRepository = FakeAuthRepository(authenticatedSession),
            remoteGroupRepository = FakeRemoteGroupRepository(
                failure = { GroupSyncException("No connection to the Tally server.") },
            ),
        )
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onSyncGroupClick()

        val stateAfterFailure = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.syncAction is GroupSyncActionUiState.Failed }
                as GroupDetailUiState.Content
        val syncAction = stateAfterFailure.syncAction as GroupSyncActionUiState.Failed
        assertEquals("No connection to the Tally server.", syncAction.message)

        val persisted = database.groupDao().getGroupById(groupId)!!
        assertFalse("a network failure must not leave the group marked as synced", persisted.isSynced)
        assertNull(persisted.remoteId)
    }

    /**
     * Marks the group as synced by reinserting the same row with `isSynced=true` — the same
     * production path ([GroupDetailViewModel.syncGroup]). Before `GroupDao.insert` became
     * `@Upsert`, this would silently wipe out participants/expenses via `ON DELETE CASCADE` (SQLite
     * `INSERT OR REPLACE` is a DELETE+INSERT); `@Upsert` does a real `UPDATE`, so this helper today
     * is just a test shortcut, not a workaround.
     */
    private suspend fun markGroupAsSynced(groupId: String, remoteId: String) {
        val group = requireNotNull(groupRepository.getGroupById(groupId))
        groupRepository.insertGroup(group.copy(isSynced = true, remoteId = remoteId))
    }

    // --- Settlement history ---

    @Test
    fun `recorded settlements show up in the history, most recent first`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        settlementRepository.insertSettlement(
            Settlement(
                id = "s1",
                groupId = groupId,
                payerId = "p2",
                receiverId = "p1",
                amount = Money.ofCents(333),
                createdAt = Instant.ofEpochMilli(1_000),
            ),
        )
        settlementRepository.insertSettlement(
            Settlement(
                id = "s2",
                groupId = groupId,
                payerId = "p3",
                receiverId = "p1",
                amount = Money.ofCents(333),
                createdAt = Instant.ofEpochMilli(2_000),
            ),
        )
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.settlements.size == 2 } as GroupDetailUiState.Content

        assertEquals(listOf("s2", "s1"), state.settlements.map { it.id })
        assertEquals("Diego", state.settlements.first().payerName)
        assertEquals("You", state.settlements.first().receiverName)
        assertEquals(333L, state.settlements.first().amountCents)
    }

    @Test
    fun `a group with no settlements has an empty history`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(groupId)

        val state = viewModel.uiState.first { it is GroupDetailUiState.Content } as GroupDetailUiState.Content

        assertTrue(state.settlements.isEmpty())
    }

    // --- Delete expense ---

    @Test
    fun `deleting a local expense removes it from Room and recalculates the balance`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val viewModel = buildViewModel(groupId)
        viewModel.uiState.first { it is GroupDetailUiState.Content && it.expenses.isNotEmpty() }

        viewModel.onDeleteExpenseClick("e1")

        val stateAfterDelete = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.expenses.isEmpty() } as GroupDetailUiState.Content
        assertTrue("with no expenses, everyone goes back to settled", stateAfterDelete.balances.all { it.balance is ParticipantBalance.Settled })
        assertTrue(expenseRepository.getExpensesFlow(groupId).first().isEmpty())
    }

    @Test
    fun `deleting an expense from an unsynced group does not call the backend`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        val remoteExpenseRepository = FakeRemoteExpenseRepository()
        val viewModel = buildViewModel(groupId, remoteExpenseRepository = remoteExpenseRepository)
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onDeleteExpenseClick("e1")
        viewModel.uiState.first { it is GroupDetailUiState.Content && it.expenses.isEmpty() }

        assertNull("a local (unsynced) group should never try to talk to the backend", remoteExpenseRepository.lastDeletedExpenseId)
    }

    @Test
    fun `deleting an expense from a synced group propagates the deletion to the backend`() = runTest(testDispatcher) {
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
    fun `a network failure while propagating a deletion does not undo the local removal`() = runTest(testDispatcher) {
        val groupId = seedGroupWithExpense()
        markGroupAsSynced(groupId, remoteId = "remote-$groupId")
        val remoteExpenseRepository = FakeRemoteExpenseRepository(
            failure = { GroupSyncException("No connection to the Tally server.") },
        )
        val viewModel = buildViewModel(groupId, remoteExpenseRepository = remoteExpenseRepository)
        viewModel.uiState.first { it is GroupDetailUiState.Content }

        viewModel.onDeleteExpenseClick("e1")
        remoteExpenseRepository.awaitDelete() // waits for the (about to fail) propagation attempt to happen.

        val stateAfterDelete = viewModel.uiState
            .first { it is GroupDetailUiState.Content && it.expenses.isEmpty() } as GroupDetailUiState.Content
        assertTrue("the local deletion stands regardless of the network failure (local-first)", stateAfterDelete.expenses.isEmpty())
    }

    // GroupDetailViewModel.startRealtimeUpdates()/stopRealtimeUpdates() only orchestrate
    // WHEN to connect/disconnect — the connection logic itself (SignalRGroupRealtimeGateway) is
    // tested separately in :data, with no HubConnection at all (see GroupEventRecorderTest/
    // MissedGroupEventsSynchronizerTest). Here we only verify the gateway is called with the right
    // ids, under the right conditions.

    @Test
    fun `startRealtimeUpdates connects the gateway with localGroupId and remoteId when the group is synced and authenticated`() =
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
    fun `startRealtimeUpdates does not connect the gateway when the group is not synced`() = runTest(testDispatcher) {
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

        assertTrue("an unsynced group cannot open a connection (it has no remoteId)", gateway.connectCalls.isEmpty())
    }

    @Test
    fun `startRealtimeUpdates does not connect the gateway when there is no authenticated session`() = runTest(testDispatcher) {
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

        assertTrue("with no session, a connection cannot be opened", gateway.connectCalls.isEmpty())
    }

    @Test
    fun `stopRealtimeUpdates disconnects an active connection`() = runTest(testDispatcher) {
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
     * [awaitConnect] exists because [com.tally.domain.repository.GroupRepository.getGroupsFlow]
     * (Room) delivers its first emission from a real executor, outside the
     * `TestCoroutineScheduler`'s control — `testDispatcher.scheduler.advanceUntilIdle()` doesn't
     * reliably wait for that (a race: nothing guarantees Room's query has already finished when
     * `advanceUntilIdle()` runs). Really suspending on a `Channel` is the deterministic way to wait
     * for the first `connect()` to happen.
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
            throw UnsupportedOperationException("not used in this test")

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
            throw UnsupportedOperationException("not used in this test — see JoinGroupViewModelTest")
        }
    }

    /**
     * [awaitDelete] exists for the same reason as [FakeGroupRealtimeGateway.awaitConnect]:
     * [GroupDetailViewModel.onDeleteExpenseClick] fires inside its own `viewModelScope.launch`, and
     * the expenses `Flow` (Room) may have already emitted "empty list" — the signal the tests use
     * to know the local deletion happened — before the backend propagation call, further along in
     * the same coroutine, has run. Really suspending on a `Channel` is the deterministic way to
     * wait for the propagation attempt (success or failure) to happen.
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
            throw UnsupportedOperationException("not used in this test — see CreateExpenseViewModelTest")
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
