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
 * Confirma que insert+query funcionam contra um Room de verdade (não mock) — entregável de
 * T7.2. Roda via Robolectric (JVM) porque este ambiente não tem emulador/dispositivo conectado
 * para um teste instrumentado (`androidTest`); banco em memória, sem tocar disco.
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
    fun `insere grupo e le de volta pelo GroupDao`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)

        database.groupDao().insert(group)

        assertEquals(group, database.groupDao().getGroupById("g1"))
        assertEquals(listOf(group), database.groupDao().getGroupsFlow().first())
    }

    @Test
    fun `insere participante vinculado ao grupo e le de volta pelo ParticipantDao`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val participant = ParticipantEntity(id = "p1", groupId = "g1", name = "Você", isYou = true)

        database.groupDao().insert(group)
        database.participantDao().insert(participant)

        assertEquals(listOf(participant), database.participantDao().getParticipantsFlow("g1").first())
    }

    @Test
    fun `remove grupo em cascata remove participantes do grupo`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val participant = ParticipantEntity(id = "p1", groupId = "g1", name = "Você", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(participant)

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<ParticipantEntity>(), database.participantDao().getParticipantsFlow("g1").first())
    }

    @Test
    fun `GroupEntity guarda isSynced (T7B) e le de volta pelo GroupDao`() = runTest {
        val synced = GroupEntity(id = "g2", name = "Viagem", createdAtEpochMillis = 2_000L, isSynced = true)

        database.groupDao().insert(synced)

        assertTrue(database.groupDao().getGroupById("g2")!!.isSynced)
    }

    @Test
    fun `insertWithSplits grava despesa e partes, getExpensesWithSplitsFlow le as duas juntas`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val other = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(other)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
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
    fun `insertWithSplits substitui as partes antigas em vez de acumular`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(payer)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
            amountCents = 1000,
            paidByParticipantId = "p1",
            createdAtEpochMillis = 3_000L,
        )
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.WEIGHT, weight = 1)),
        )

        // Edição da mesma despesa (mesmo id) com uma divisão diferente — as partes antigas não
        // podem sobrar como lixo órfão (ver comentário em ExpenseDao.insertWithSplits).
        database.expenseDao().insertWithSplits(
            expense,
            listOf(ExpenseSplitEntity(expenseId = "e1", participantId = "p1", type = SplitTypeEntity.EQUAL)),
        )

        val row = database.expenseDao().getExpensesWithSplitsFlow("g1").first().single()
        assertEquals(1, row.splits.size)
        assertEquals(SplitTypeEntity.EQUAL, row.splits.single().type)
    }

    @Test
    fun `remove despesa em cascata remove suas partes`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        database.groupDao().insert(group)
        database.participantDao().insert(payer)

        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
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
    fun `insere quitacao vinculada ao grupo e le de volta pelo SettlementDao (T42-1)`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val receiver = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(receiver)

        val settlement = SettlementEntity(id = "s1", groupId = "g1", payerId = "p1", receiverId = "p2", amountCents = 500)
        database.settlementDao().insert(settlement)

        assertEquals(listOf(settlement), database.settlementDao().getSettlementsFlow("g1").first())
    }

    @Test
    fun `remove grupo em cascata remove quitacoes do grupo`() = runTest {
        val group = GroupEntity(id = "g1", name = "Churras de sábado", createdAtEpochMillis = 1_000L)
        val payer = ParticipantEntity(id = "p1", groupId = "g1", name = "P1", isYou = true)
        val receiver = ParticipantEntity(id = "p2", groupId = "g1", name = "P2")
        database.groupDao().insert(group)
        database.participantDao().insert(payer)
        database.participantDao().insert(receiver)
        database.settlementDao().insert(
            SettlementEntity(id = "s1", groupId = "g1", payerId = "p1", receiverId = "p2", amountCents = 500),
        )

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<SettlementEntity>(), database.settlementDao().getSettlementsFlow("g1").first())
    }

    @Test
    fun `insere notificacao vinculada ao grupo e le de volta pelo NotificationDao (T40-1)`() = runTest {
        val group = GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)

        val notification = NotificationEntity(
            id = "despesa:e1",
            groupId = "g1",
            message = "Duda lançou \"Mercado\" — R$ 30,00 — em \"Viagem pra praia\".",
            occurredAtEpochMillis = 5_000L,
            isRead = false,
        )
        database.notificationDao().insert(notification)

        assertEquals(listOf(notification), database.notificationDao().getNotificationsFlow().first())
    }

    @Test
    fun `insert com id repetido e OnConflictStrategy IGNORE nao sobrescreve isRead ja marcado`() = runTest {
        val group = GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "g1", message = "original", occurredAtEpochMillis = 5_000L, isRead = false),
        )
        database.notificationDao().markAllAsRead()

        // Mesmo evento chegando de novo (tempo real + pull de reconexão, T40.2) — não pode
        // reverter isRead=true de volta pra false.
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "g1", message = "original", occurredAtEpochMillis = 5_000L, isRead = false),
        )

        assertTrue(database.notificationDao().getNotificationsFlow().first().single().isRead)
    }

    @Test
    fun `getUnreadCountFlow conta so as nao lidas`() = runTest {
        val group = GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "quitacao:s1", groupId = "g1", message = "b", occurredAtEpochMillis = 2_000L, isRead = true),
        )

        assertEquals(1, database.notificationDao().getUnreadCountFlow().first())
    }

    @Test
    fun `getLastEventEpochMillis devolve o maior timestamp entre as notificacoes gravadas`() = runTest {
        val group = GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )
        database.notificationDao().insert(
            NotificationEntity(id = "quitacao:s1", groupId = "g1", message = "b", occurredAtEpochMillis = 9_000L, isRead = false),
        )

        assertEquals(9_000L, database.notificationDao().getLastEventEpochMillis())
    }

    @Test
    fun `remove grupo em cascata remove notificacoes do grupo`() = runTest {
        val group = GroupEntity(id = "g1", name = "Viagem pra praia", createdAtEpochMillis = 1_000L)
        database.groupDao().insert(group)
        database.notificationDao().insert(
            NotificationEntity(id = "despesa:e1", groupId = "g1", message = "a", occurredAtEpochMillis = 1_000L, isRead = false),
        )

        database.groupDao().deleteById("g1")

        assertEquals(emptyList<NotificationEntity>(), database.notificationDao().getNotificationsFlow().first())
    }
}
