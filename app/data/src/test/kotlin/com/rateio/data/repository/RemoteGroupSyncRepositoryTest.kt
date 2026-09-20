package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.DespesaSincronizadaDto
import com.rateio.data.remote.groups.GroupsApi
import com.rateio.data.remote.groups.SincronizarGrupoRequestDto
import com.rateio.data.remote.groups.SincronizarGrupoResponseDto
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupSyncException
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cobre T19.1: [RemoteGroupSyncRepository] espelha `POST /groups/sync` (T18) com o
 * `Authorization: Bearer` lido de [TokenStorage] e traduz falha de rede/HTTP (ou sessão ausente)
 * para [GroupSyncException] — nunca deixa `HttpException`/`IOException` vazar. Mesmo padrão de
 * `RemoteAuthRepositoryTest`: dublês simples de [GroupsApi]/[TokenStorage] em vez de framework de
 * mock (nenhum está nas dependências de teste de `:data`).
 */
class RemoteGroupSyncRepositoryTest {

    private val group = Group(
        id = "g1",
        name = "Churras de sábado",
        createdAt = Instant.parse("2026-01-10T12:00:00Z"),
    )
    private val participants = listOf(
        Participant(id = "p1", groupId = "g1", name = "Você", isYou = true),
        Participant(id = "p2", groupId = "g1", name = "Marina"),
    )
    private val expenses = listOf(
        Expense(
            id = "e1",
            groupId = "g1",
            description = "Churrasco",
            amountCents = 10_000,
            paidByParticipantId = "p1",
            createdAt = Instant.parse("2026-01-11T12:00:00Z"),
            splits = listOf(
                ExpenseSplit.Equal(participantId = "p1"),
                ExpenseSplit.Equal(participantId = "p2"),
            ),
        ),
    )
    private val session = AuthSession(
        accessToken = "access-token-valido",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `syncGroup envia Authorization Bearer com o access token da sessao`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.syncGroup(group, participants, expenses)

        assertEquals("Bearer access-token-valido", groupsApi.lastBearerToken)
    }

    @Test
    fun `syncGroup mapeia grupo, participantes e despesas pro payload do backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        val remoteId = repository.syncGroup(group, participants, expenses)

        assertEquals("remote-grupo-id", remoteId)
        val request = requireNotNull(groupsApi.lastRequest)
        assertEquals("Churras de sábado", request.nome)
        assertEquals(listOf("p1", "p2"), request.participantes.map { it.id })
        assertTrue("todo participante local sincroniza como convidado", request.participantes.all { it.ehConvidado })
        assertEquals(1, request.despesas.size)
        val despesa = request.despesas.single()
        assertEquals("2026-01-11", despesa.data)
        assertEquals(2, despesa.participacoes.size)
    }

    @Test
    fun `syncGroup sem sessao lanca GroupSyncException sem chamar o backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("nao deve nem tentar chamar o backend sem sessao", groupsApi.lastRequest)
        }
    }

    @Test
    fun `syncGroup traduz falha de rede para GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("sem conexão com o servidor do Rateio") })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `syncGroup traduz erro HTTP para GroupSyncException`() = runTest {
        val corpoDeErro = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, corpoDeErro)) })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.syncGroup(group, participants, expenses)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    @Test
    fun `joinByCode envia Authorization Bearer e o codigo do convite`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        val remoteId = repository.joinByCode("ABC123")

        assertEquals("Bearer access-token-valido", groupsApi.lastBearerToken)
        assertEquals("ABC123", groupsApi.lastCodigo)
        assertEquals("remote-grupo-id", remoteId)
    }

    @Test
    fun `joinByCode sem sessao lanca GroupSyncException sem chamar o backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.joinByCode("ABC123")
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("nao deve nem tentar chamar o backend sem sessao", groupsApi.lastCodigo)
        }
    }

    @Test
    fun `joinByCode traduz falha de rede para GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("sem conexão com o servidor do Rateio") })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.joinByCode("ABC123")
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `joinByCode traduz codigo invalido (erro HTTP) para GroupSyncException`() = runTest {
        val corpoDeErro = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, corpoDeErro)) })
        val repository = RemoteGroupSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.joinByCode("CODIGO-INVALIDO")
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    private class FakeGroupsApi(private val failure: (() -> Throwable)? = null) : GroupsApi {
        var lastBearerToken: String? = null
            private set
        var lastRequest: SincronizarGrupoRequestDto? = null
            private set
        var lastCodigo: String? = null
            private set

        override suspend fun sync(
            bearerToken: String,
            request: SincronizarGrupoRequestDto,
        ): SincronizarGrupoResponseDto {
            lastBearerToken = bearerToken
            lastRequest = request
            failure?.invoke()?.let { throw it }
            return SincronizarGrupoResponseDto(grupoId = "remote-grupo-id")
        }

        override suspend fun join(bearerToken: String, codigo: String): SincronizarGrupoResponseDto {
            lastBearerToken = bearerToken
            lastCodigo = codigo
            failure?.invoke()?.let { throw it }
            return SincronizarGrupoResponseDto(grupoId = "remote-grupo-id")
        }

        // Não usados por RemoteGroupSyncRepositoryTest (T29 os cobre em
        // RemoteExpenseSyncRepositoryTest) — só aqui pra satisfazer a interface GroupsApi.
        override suspend fun updateExpense(
            bearerToken: String,
            id: String,
            expenseId: String,
            request: DespesaSincronizadaDto,
        ): Unit = throw UnsupportedOperationException("não usado neste teste")

        override suspend fun deleteExpense(bearerToken: String, id: String, expenseId: String): Unit =
            throw UnsupportedOperationException("não usado neste teste")
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
}
