package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.auth.GoogleLoginRequestDto
import com.rateio.data.remote.auth.GoogleLoginResponseDto
import com.rateio.data.remote.auth.LogoutRequestDto
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import java.io.IOException
import kotlinx.coroutines.flow.first
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
 * Cobre T14.2: `RemoteAuthRepository.signOut` sempre limpa a sessão local — mesmo quando a
 * revogação no backend (`POST /auth/logout`, T14.1) falha por falta de rede ou erro HTTP — porque
 * o usuário não pode ficar preso logado no aparelho por falha de rede (logout local tem
 * prioridade sobre o de servidor, ver doc de `RemoteAuthRepository.signOut`).
 *
 * Usa dublês simples de [AuthApi]/[TokenStorage] em vez de um framework de mock: nenhum está nas
 * dependências de teste de `:data` hoje, e ambas são interfaces pequenas o bastante pra não
 * justificar a dependência nova.
 */
class RemoteAuthRepositoryTest {

    private val session = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token-do-usuario",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `signOut chama logout no backend com o refresh token da sessao atual`() = runTest {
        val authApi = FakeAuthApi()
        val repository = RemoteAuthRepository(authApi, FakeTokenStorage(initialSession = session))

        repository.signOut()

        assertEquals(listOf("refresh-token-do-usuario"), authApi.logoutCalls)
    }

    @Test
    fun `signOut sem sessao atual nao chama o backend`() = runTest {
        val authApi = FakeAuthApi()
        val repository = RemoteAuthRepository(authApi, FakeTokenStorage(initialSession = null))

        repository.signOut()

        assertTrue(authApi.logoutCalls.isEmpty())
    }

    @Test
    fun `signOut limpa sessao local mesmo quando o backend esta sem rede`() = runTest {
        val authApi = FakeAuthApi(logoutFailure = { IOException("sem conexão com o servidor do Rateio") })
        val tokenStorage = FakeTokenStorage(initialSession = session)
        val repository = RemoteAuthRepository(authApi, tokenStorage)

        repository.signOut()

        assertTrue(tokenStorage.cleared)
        assertNull(repository.getSessionFlow().first())
    }

    @Test
    fun `signOut limpa sessao local mesmo quando o backend responde com erro HTTP`() = runTest {
        val corpoDeErro = "".toResponseBody("application/json".toMediaType())
        val authApi = FakeAuthApi(logoutFailure = { HttpException(Response.error<Unit>(401, corpoDeErro)) })
        val tokenStorage = FakeTokenStorage(initialSession = session)
        val repository = RemoteAuthRepository(authApi, tokenStorage)

        repository.signOut()

        assertTrue(tokenStorage.cleared)
        assertNull(repository.getSessionFlow().first())
    }

    private class FakeAuthApi(private val logoutFailure: (() -> Throwable)? = null) : AuthApi {
        val logoutCalls = mutableListOf<String>()

        override suspend fun loginWithGoogle(request: GoogleLoginRequestDto): GoogleLoginResponseDto =
            throw UnsupportedOperationException("não usado neste teste")

        override suspend fun logout(request: LogoutRequestDto) {
            logoutCalls += request.refreshToken
            logoutFailure?.invoke()?.let { throw it }
        }
    }

    private class FakeTokenStorage(initialSession: AuthSession?) : TokenStorage {
        private var stored = initialSession
        var cleared = false
            private set

        override fun read(): AuthSession? = stored

        override fun save(session: AuthSession) {
            stored = session
            cleared = false
        }

        override fun clear() {
            stored = null
            cleared = true
        }
    }
}
