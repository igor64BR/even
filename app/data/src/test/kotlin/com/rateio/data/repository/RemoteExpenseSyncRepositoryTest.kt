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
 * Cobre T29 (app: editar/excluir despesa) contra T28 (backend, `PUT`/`DELETE
 * /groups/{id}/expenses/{expenseId}`, rodando em paralelo — ver a pendência documentada em
 * [com.rateio.domain.repository.RemoteExpenseRepository]). Mesmo padrão de dublê simples de
 * [RemoteGroupSyncRepositoryTest] (T19.1): sem framework de mock, [FakeGroupsApi]/[FakeTokenStorage]
 * já usados lá.
 */
class RemoteExpenseSyncRepositoryTest {

    private val expense = Expense(
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
    )
    private val session = AuthSession(
        accessToken = "access-token-valido",
        refreshToken = "refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `updateExpense envia Authorization Bearer e o payload mapeado da despesa`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)

        assertEquals("Bearer access-token-valido", groupsApi.lastBearerToken)
        assertEquals("remote-g1", groupsApi.lastGroupId)
        assertEquals("e1", groupsApi.lastExpenseId)
        val request = requireNotNull(groupsApi.lastUpdateRequest)
        assertEquals("Churrasco", request.descricao)
        assertEquals(10_000L, request.valorTotalCentavos)
        assertEquals(2, request.participacoes.size)
    }

    @Test
    fun `updateExpense sem sessao lanca GroupSyncException sem chamar o backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("nao deve nem tentar chamar o backend sem sessao", groupsApi.lastUpdateRequest)
        }
    }

    @Test
    fun `updateExpense traduz falha de rede para GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("sem conexão com o servidor do Rateio") })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun `updateExpense traduz erro HTTP para GroupSyncException`() = runTest {
        val corpoDeErro = "".toResponseBody("application/json".toMediaType())
        val groupsApi = FakeGroupsApi(failure = { HttpException(Response.error<Unit>(400, corpoDeErro)) })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.updateExpense(remoteGroupId = "remote-g1", expense = expense)
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is HttpException)
        }
    }

    @Test
    fun `deleteExpense envia Authorization Bearer com os ids certos`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")

        assertEquals("Bearer access-token-valido", groupsApi.lastBearerToken)
        assertEquals("remote-g1", groupsApi.lastGroupId)
        assertEquals("e1", groupsApi.lastExpenseId)
    }

    @Test
    fun `deleteExpense sem sessao lanca GroupSyncException sem chamar o backend`() = runTest {
        val groupsApi = FakeGroupsApi()
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(initialSession = null))

        try {
            repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertNull("nao deve nem tentar chamar o backend sem sessao", groupsApi.lastExpenseId)
        }
    }

    @Test
    fun `deleteExpense traduz falha de rede para GroupSyncException`() = runTest {
        val groupsApi = FakeGroupsApi(failure = { IOException("sem conexão com o servidor do Rateio") })
        val repository = RemoteExpenseSyncRepository(groupsApi, FakeTokenStorage(session))

        try {
            repository.deleteExpense(remoteGroupId = "remote-g1", expenseId = "e1")
            fail("esperava GroupSyncException")
        } catch (error: GroupSyncException) {
            assertTrue(error.cause is IOException)
        }
    }

    private class FakeGroupsApi(private val failure: (() -> Throwable)? = null) : GroupsApi {
        var lastBearerToken: String? = null
            private set
        var lastGroupId: String? = null
            private set
        var lastExpenseId: String? = null
            private set
        var lastUpdateRequest: DespesaSincronizadaDto? = null
            private set

        override suspend fun sync(
            bearerToken: String,
            request: SincronizarGrupoRequestDto,
        ): SincronizarGrupoResponseDto = throw UnsupportedOperationException("não usado neste teste")

        override suspend fun join(bearerToken: String, codigo: String): SincronizarGrupoResponseDto =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun updateExpense(
            bearerToken: String,
            id: String,
            expenseId: String,
            request: DespesaSincronizadaDto,
        ) {
            lastBearerToken = bearerToken
            lastGroupId = id
            lastExpenseId = expenseId
            lastUpdateRequest = request
            failure?.invoke()?.let { throw it }
        }

        override suspend fun deleteExpense(bearerToken: String, id: String, expenseId: String) {
            lastBearerToken = bearerToken
            lastGroupId = id
            lastExpenseId = expenseId
            failure?.invoke()?.let { throw it }
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
}
