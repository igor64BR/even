package com.rateio.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.ParticipantEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
}
