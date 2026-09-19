package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.auth.GoogleLoginRequestDto
import com.rateio.data.remote.auth.GoogleLoginResponseDto
import com.rateio.domain.model.AuthSession
import com.rateio.domain.model.AuthenticatedUser
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.AuthenticationFailedException
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.HttpException

/**
 * Implementação de [AuthRepository] sobre [AuthApi] (`POST /auth/google`, T11) + [TokenStorage]
 * (T12.2). Traduz falhas de Retrofit/OkHttp para [AuthenticationFailedException] — `AuthViewModel`
 * nunca vê um tipo de rede, só a mensagem já pronta pra tela.
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

    override suspend fun signOut() {
        tokenStorage.clear()
        sessionState.value = null
    }

    private suspend fun requestSession(googleIdToken: String): AuthSession {
        try {
            return authApi.loginWithGoogle(GoogleLoginRequestDto(googleIdToken)).toDomain()
        } catch (error: HttpException) {
            throw AuthenticationFailedException("O Google não confirmou essa conta.", error)
        } catch (error: IOException) {
            throw AuthenticationFailedException("Sem conexão com o servidor do Rateio.", error)
        }
    }
}

private fun GoogleLoginResponseDto.toDomain() = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = AuthenticatedUser(name = user.nome, email = user.email),
)
