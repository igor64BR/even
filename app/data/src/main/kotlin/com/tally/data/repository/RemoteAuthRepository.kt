package com.tally.data.repository

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.auth.AuthApi
import com.tally.data.remote.auth.GoogleLoginRequestDto
import com.tally.data.remote.auth.GoogleLoginResponseDto
import com.tally.data.remote.auth.LogoutRequestDto
import com.tally.domain.model.AuthSession
import com.tally.domain.model.AuthenticatedUser
import com.tally.domain.repository.AuthRepository
import com.tally.domain.repository.AuthenticationFailedException
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.HttpException

/**
 * Implementation of [AuthRepository] on top of [AuthApi] (`POST /auth/google`; `POST
 * /auth/logout`) + [TokenStorage]. Translates Retrofit/OkHttp failures into
 * [AuthenticationFailedException] — `AuthViewModel` never sees a network type, only the message
 * already ready for the screen.
 */
class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val sessionState = MutableStateFlow(tokenStorage.read())

    override fun getSessionFlow(): Flow<AuthSession?> = sessionState

    override suspend fun signInWithGoogle(googleIdToken: String): AuthSession {
        val session = requestSession(googleIdToken)
        tokenStorage.save(session)
        sessionState.value = session
        return session
    }

    /**
     * Revokes the session on the backend when there's one to revoke, but the local session
     * is ALWAYS cleared, even if the network call fails — the user can't be stuck signed in on the
     * device just because there's no connection. Local logout takes priority over the server one.
     */
    override suspend fun signOut() {
        revokeSessionOnServer()
        tokenStorage.clear()
        sessionState.value = null
    }

    private suspend fun requestSession(googleIdToken: String): AuthSession {
        try {
            return authApi.loginWithGoogle(GoogleLoginRequestDto(googleIdToken)).toDomain()
        } catch (error: HttpException) {
            throw AuthenticationFailedException("Google didn't confirm that account.", error)
        } catch (error: IOException) {
            throw AuthenticationFailedException("No connection to the Tally server.", error)
        }
    }

    /**
     * Best-effort: with no current session, there's nothing to revoke. With a session, it tries to
     * revoke but swallows any network/server failure — the caller (`signOut`) proceeds and clears
     * the local session anyway (see `signOut`'s doc).
     */
    private suspend fun revokeSessionOnServer() {
        val refreshToken = sessionState.value?.refreshToken ?: return
        try {
            authApi.logout(LogoutRequestDto(refreshToken))
        } catch (error: HttpException) {
            // Server refused (e.g. session already revoked) — irrelevant to the local logout.
        } catch (error: IOException) {
            // No connection to the Tally server — local logout still takes priority.
        }
    }
}

private fun GoogleLoginResponseDto.toDomain() = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = AuthenticatedUser(name = user.name, email = user.email),
)
