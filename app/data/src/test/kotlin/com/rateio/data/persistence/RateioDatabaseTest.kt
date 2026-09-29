package com.rateio.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.entity.ExpenseEntity
import com.rateio.data.persistence.entity.ExpenseSplitEntity
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.NotificationEntity
import com.rateio.data.persistence.entity.ParticipantEntity
import com.rateio.data.persistence.entity.SettlementEntity
import com.rateio.data.persistence.entity.SplitTypeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Confirms that insert+query work against a real Room (not a mock) — a deliverable of
 * T7.2. Runs via Robolectric (JVM) because this environment has no connected emulator/device for
 * an instrumented test (`androidTest`); an in-memory database, no disk access.
 */
@RunWith(RobolectricTestRunner::class)
class RateioDatabaseTest {

    private lateinit var database: RateioDatabase

    @Before
    fun createInMemoryDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RateioDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `inserts a group and reads it back through GroupDao`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)

        database.groupDao().insert(group)

        assertEquals(group, database.groupDao().getGroupById("g1"))
        assertEquals(listOf(group), database.groupDao().getGroupsFlow().first())
    }

    @Test
    fun `re-inserting an existing group (e-g- marking isSynced) does not delete its participants and expenses`() = runTest {
        // Regression: GroupDao.insert used @Insert(REPLACE), which in SQLite is a DELETE+INSERT
        // and triggered ON DELETE CASCADE of the participant/expense/settlement/notification FKs
        // every time the already-existing group was re-inserted (e.g.
        // GroupDetailViewModel.syncGroup() writing isSynced=true) — found during T29. @Upsert does
        // a real UPDATE.
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAtEpochMillis = 3_000L,
        )
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.EQUAL)),
        )

        database.groupDao().insert(group.copy(isSynced = true, remoteId = "remote-1"))

        assertTrue(database.groupDao().getGroupById("g1")!!.isSynced)
        assertEquals(listOf(payer), database.participantDao().getParticipantsFlow("g1").first())
        assertEquals(1, database.expenseDao().getExpensesWithSplitsFlow("g1").first().size)
    }

    @Test
    fun `inserts a participant linked to the group and reads it back through ParticipantDao`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val participant = ParticipantEntity(id = "p1", groupId = "g1", name = "You", isYou = true)

        database.groupDao().insert(group)
        database.participantDao().insert(participant)

        assertEquals(listOf(participant), database.participantDao().getParticipantsFlow("g1").first())
    }

    @Test
    fun `deleting a group cascades and removes its participants`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val participant = ParticipantEntity(id = "p1", groupId = "g1", name = "You", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(participant)

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<ParticipantEntity>(), database.participantDao().getParticipantsFlow("g1").first())
    }

    @Test
    fun `GroupEntity stores isSynced (T7B) and reads it back through GroupDao`() = runTest {
        val synced = GroupEntity(id = "g2", name = "Trip", createdAtEpochMillis = 2_000L, isSynced = true)

        database.groupDao().insert(synced)

        assertTrue(database.groupDao().getGroupById("g2")!!.isSynced)
    }

    @Test
    fun `insertWithSplits stores the expense and its splits, getExpensesWithSplitsFlow reads both together`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val other = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(other)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAtEpochMillis = 3_000L,
        )
        val splits = listOf(
            ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.EQUAL),
            ExpenseSplitEntity(expenseId = "e1", participantId = "p2", type = SplitTypeEntity.EQUAL),
        )

        database.expenseDao().insertWithSplits(expense, splits)

        val rows = database.expenseDao().getExpensesWithSplitsFlow("g1").first()
        val row = rows.single()
        assertEquals(expense, row.expense)
        assertEquals(splits.toSet(), row.splits.toSet())
    }

    @Test
    fun `insertWithSplits replaces the old splits instead of accumulating them`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(payer)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAtEpochMillis = 3_000L,
        )
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.WEIGHT, weight = 1)),
        )

        // Editing the same expense (same id) with a different split — the old splits must not be
        // left behind as orphaned junk (see the comment in ExpenseDao.insertWithSplits).
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.EQUAL)),
        )

        val row = database.expenseDao().getExpensesWithSplitsFlow("g1").first().single()
        assertEquals(1, row.splits.size)
        assertEquals(SplitTypeEntity.EQUAL, row.splits.single().type)
    }

    @Test
    fun `deleting an expense cascades and removes its splits`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(payer)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Barbecue",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAtEpochMillis = 3_000L,
        )
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.EQUAL)),
        )

        database.expenseDao().deleteById("e1")

        assertEquals(emptyList<Any>(), database.expenseDao().getExpensesWithSplitsFlow("g1").first())
    }

    @Test
    fun `inserts a settlement linked to the group and reads it back through SettlementDao (T42-1)`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val receiver = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(receiver)

        val settlement = SettlementEntity(id = "s1", groupId = "g1", payerId = "p1", receiverId = "p2", amountCents = 500, createdAtEpochMillis = 4_000L)
        database.settlementDao().insert(settlement)

        assertEquals(listOf(settlement), database.settlementDao().getSettlementsFlow("g1").first())
    }

    @Test
    fun `deleting a group cascades and removes the group's settlements`() = runTest {
        val group = GroupEntity(id = "g1", name = "Saturday barbecue", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val receiver = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(receiver)
        database.settlementDao().insert(
            SettlementEntity(id = "s1", groupId = "g1", payerId = "p1", receiverId = "p2", amountCents = 500, createdAtEpochMillis = 4_000L),
        )

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<SettlementEntity>(), database.settlementDao().getSettlementsFlow("g1").first())
    }

    @Test
    fun `inserts a notification linked to the group and reads it back through NotificationDao (T40-1)`() = runTest {
        val group = GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)

        val notification = NotificationEntity(
            id = "expense:e1",
            groupId = "g1",
            message = "Duda logged \"Groceries\" — R$ 30,00 — in \"Beach trip\".",
            occurredAtEpochMillis = 5_000L,
            isRead = false,
        )
        database.notificationDao().insert(notification)

        assertEquals(listOf(notification), database.notificationDao().getNotificationsFlow().first())
    }

    @Test
    fun `insert with a repeated id and OnConflictStrategy IGNORE does not overwrite an already-marked isRead`() = runTest {
        val group = GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "g1", message = "original", occurredAtEpochMillis = 5_000L, isRead = false),
        )
        database.notificationDao().markAllAsRead()

        // The same event arriving again (live + reconnect pull, T40.2) — must not revert
        // isRead=true back to false.
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "g1", message = "original", occurredAtEpochMillis = 5_000L, isRead = false),
        )

        assertTrue(database.notificationDao().getNotificationsFlow().first().single().isRead)
    }

    @Test
    fun `getUnreadCountFlow only counts the unread ones`() = runTest {
        val group = GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "settlement:s1", groupId = "g1", message = "b", occurredAtEpochMillis = 2_000L, isRead = true),
        )

        assertEquals(1, database.notificationDao().getUnreadCountFlow().first())
    }

    @Test
    fun `getLastEventEpochMillis returns the largest timestamp among the recorded notifications`() = runTest {
        val group = GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "settlement:s1", groupId = "g1", message = "b", occurredAtEpochMillis = 9_000L, isRead = false),
        )

        assertEquals(9_000L, database.notificationDao().getLastEventEpochMillis())
    }

    @Test
    fun `deleting a group cascades and removes the group's notifications`() = runTest {
        val group = GroupEntity(id = "g1", name = "Beach trip", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "expense:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<NotificationEntity>(), database.notificationDao().getNotificationsFlow().first())
    }
}
