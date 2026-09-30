package com.tally.data.repository

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.auth.AuthApi
import com.tally.data.remote.auth.GoogleLoginRequestDto
import com.tally.data.remote.auth.GoogleLoginResponseDto
import com.tally.data.remote.auth.LogoutRequestDto
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser
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
 * Covers `RemoteAuthRepository.signOut` always clears the local session — even when the
 * backend revocation (`POST /auth/logout`) fails due to no network or an HTTP error —
 * because the user can't be stuck signed in on the device due to a network failure (local logout
 * takes priority over the server one, see `RemoteAuthRepository.signOut`'s doc).
 *
 * Uses simple test doubles for [AuthApi]/[TokenStorage] instead of a mocking framework: neither is
 * in `:data`'s test dependencies today, and both are interfaces small enough not to justify the
 * new dependency.
 */
class RemoteAuthRepositoryTest {

    private val session = AuthSession(
        accessToken = "access-token",
        refreshToken = "user-refresh-token",
        user = AuthenticatedUser(name = "Igor Baiocco", email = "igor@example.com"),
    )

    @Test
    fun `signOut calls logout on the backend with the current session's refresh token`() = runTest {
        val authApi = FakeAuthApi()
        val repository = RemoteAuthRepository(authApi, FakeTokenStorage(initialSession = session))

        repository.signOut()

        assertEquals(listOf("user-refresh-token"), authApi.logoutCalls)
    }

    @Test
    fun `signOut with no current session does not call the backend`() = runTest {
        val authApi = FakeAuthApi()
        val repository = RemoteAuthRepository(authApi, FakeTokenStorage(initialSession = null))

        repository.signOut()

        assertTrue(authApi.logoutCalls.isEmpty())
    }

    @Test
    fun `signOut clears the local session even when the backend has no network`() = runTest {
        val authApi = FakeAuthApi(logoutFailure = { IOException("no connection to the Tally server") })
        val tokenStorage = FakeTokenStorage(initialSession = session)
        val repository = RemoteAuthRepository(authApi, tokenStorage)

        repository.signOut()

        assertTrue(tokenStorage.cleared)
        assertNull(repository.getSessionFlow().first())
    }

    @Test
    fun `signOut clears the local session even when the backend responds with an HTTP error`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val authApi = FakeAuthApi(logoutFailure = { HttpException(Response.error<Unit>(401, errorBody)) })
        val tokenStorage = FakeTokenStorage(initialSession = session)
        val repository = RemoteAuthRepository(authApi, tokenStorage)

        repository.signOut()

        assertTrue(tokenStorage.cleared)
        assertNull(repository.getSessionFlow().first())
    }

    private class FakeAuthApi(private val logoutFailure: (() -> Throwable)? = null) : AuthApi {
        val logoutCalls = mutableListOf<String>()

        override suspend fun loginWithGoogle(request: GoogleLoginRequestDto): GoogleLoginResponseDto =
            throw UnsupportedOperationException("not used in this test")

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
