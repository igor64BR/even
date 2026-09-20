package com.rateio.data.remote.realtime

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.data.remote.groups.GrupoEventoDto
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.model.Group
import com.rateio.domain.model.GroupNotification
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
import com.rateio.domain.repository.ParticipantRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cobre T40.2: "reconexão busca eventos perdidos via T39" — no nível do colaborador que de fato
 * chama o endpoint, [MissedGroupEventsSynchronizer], sem `HubConnection` nenhum. [GroupEventsApi] é
 * um dublê simples (mesmo padrão de `RemoteGroupSyncRepositoryTest`); nunca foi exercitado contra
 * um `GET /groups/{id}/events` real (T39 ainda não existia no backend — lacuna documentada em
 * `GroupEventsApi`).
 */
class MissedGroupEventsSynchronizerTest {

    private val session = AuthSession(
        accessToken = "access-token-valido",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )
    private val group = Group(id = "g1", name = "Viagem pra praia", createdAt = Instant.EPOCH)
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "Você", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Duda"),
    )

    private fun buildSynchronizer(
        groupEventsApi: GroupEventsApi,
        tokenStorage: TokenStorage,
        notificationRepository: NotificationRepository = FakeNotificationRepository(),
    ) = MissedGroupEventsSynchronizer(
        groupEventsApi = groupEventsApi,
        tokenStorage = tokenStorage,
        notificationRepository = notificationRepository,
        eventRecorder = GroupEventRecorder(
            notificationRepository = notificationRepository,
            groupRepository = FakeGroupRepository(group),
            participantRepository = FakeParticipantRepository(participants),
            notificationBuilder = GroupEventNotificationBuilder(moneyFormatter = { cents -> "R$ ${cents / 100},00" }),
        ),
    )

    @Test
    fun `sync busca desde o ultimo timestamp conhecido e grava os eventos retornados`() = runTest {
        val notificationRepository = FakeNotificationRepository(
            existing = mutableListOf(
                GroupNotification(id = "n0", groupId = "g1", message = "antiga", occurredAt = Instant.ofEpochMilli(5_000)),
            ),
        )
        val api = FakeGroupEventsApi(
            events = listOf(
                GrupoEventoDto(
                    tipo = 0,
                    grupoId = "remote-g1",
                    despesaId = "e1",
                    descricao = "Mercado",
                    valorTotalCentavos = 3_000,
                    pagadorId = "p2",
                ),
            ),
        )
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session), notificationRepository)

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertEquals("Bearer access-token-valido", api.lastBearerToken)
        assertEquals("remote-g1", api.lastGroupId)
        assertEquals("1970-01-01T00:00:05Z", api.lastSince)
        val inserted = notificationRepository.inserted.single()
        assertEquals("despesa:e1", inserted.id)
        assertTrue(inserted.message.contains("Duda lançou \"Mercado\""))
    }

    @Test
    fun `sync sem nenhuma notificacao anterior manda desde Instant EPOCH (backend exige o parametro sempre)`() = runTest {
        val api = FakeGroupEventsApi(events = emptyList())
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertEquals("1970-01-01T00:00:00Z", api.lastSince)
    }

    @Test
    fun `sync sem sessao nao chama a API`() = runTest {
        val api = FakeGroupEventsApi(events = emptyList())
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(initialSession = null))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertNull("sem sessao, a chamada nem deveria acontecer", api.lastGroupId)
    }

    @Test
    fun `sync ignora evento de tipo desconhecido sem quebrar`() = runTest {
        val notificationRepository = FakeNotificationRepository()
        val api = FakeGroupEventsApi(events = listOf(GrupoEventoDto(tipo = 99, grupoId = "remote-g1")))
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session), notificationRepository)

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1")

        assertTrue(notificationRepository.inserted.isEmpty())
    }

    @Test
    fun `sync engole falha de rede sem propagar`() = runTest {
        val api = FakeGroupEventsApi(failure = { IOException("sem conexão") })
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1") // não deve lançar
    }

    @Test
    fun `sync engole erro HTTP (403 RNF07 ou 404 grupo removido) sem propagar`() = runTest {
        val corpoDeErro = "".toResponseBody("application/json".toMediaType())
        val api = FakeGroupEventsApi(failure = { HttpException(Response.error<Unit>(404, corpoDeErro)) })
        val synchronizer = buildSynchronizer(api, FakeTokenStorage(session))

        synchronizer.sync(localGroupId = "g1", remoteGroupId = "remote-g1") // não deve lançar
    }

    private class FakeGroupEventsApi(
        private val events: List<GrupoEventoDto> = emptyList(),
        private val failure: (() -> Throwable)? = null,
    ) : GroupEventsApi {
        var lastBearerToken: String? = null
            private set
        var lastGroupId: String? = null
            private set
        var lastSince: String? = null
            private set

        override suspend fun getEvents(bearerToken: String, groupId: String, since: String): List<GrupoEventoDto> {
            lastBearerToken = bearerToken
            lastGroupId = groupId
            lastSince = since
            failure?.invoke()?.let { throw it }
            return events
        }
    }

    private class FakeTokenStorage(initialSession: AuthSession?) : TokenStorage {
        private var stored = initialSession
        override fun read(): AuthSession? = stored
        override fun save(session: AuthSession) {
            stored = session
        }

        override fun clear() {
            stored = null
        }
    }

    private class FakeGroupRepository(private val group: Group?) : GroupRepository {
        override fun getGroupsFlow(): Flow<List<Group>> = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun getGroupById(groupId: String): Group? = group
        override suspend fun insertGroup(group: Group) = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun deleteGroup(groupId: String) = throw UnsupportedOperationException("não usado neste teste")
    }

    private class FakeParticipantRepository(private val participants: List<Participant>) : ParticipantRepository {
        override fun getParticipantsFlow(groupId: String): Flow<List<Participant>> = MutableStateFlow(participants)
        override suspend fun insertParticipant(participant: Participant) =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun deleteParticipant(participantId: String) =
            throw UnsupportedOperationException("não usado neste teste")
    }

    private class FakeNotificationRepository(
        val inserted: MutableList<GroupNotification> = mutableListOf(),
        existing: MutableList<GroupNotification> = mutableListOf(),
    ) : NotificationRepository {
        private val all = existing

        override fun getNotificationsFlow(): Flow<List<GroupNotification>> = MutableStateFlow(all)
        override fun getUnreadCountFlow(): Flow<Int> = MutableStateFlow(all.count { !it.isRead })
        override suspend fun insert(notification: GroupNotification) {
            inserted += notification
            all += notification
        }

        override suspend fun markAllAsRead() = throw UnsupportedOperationException("não usado neste teste")
        override suspend fun getLastEventTimestamp(): Instant? = all.maxOfOrNull { it.occurredAt }
    }
}
