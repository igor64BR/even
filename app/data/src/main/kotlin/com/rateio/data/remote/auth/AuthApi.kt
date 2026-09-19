package com.rateio.data.remote.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Espelha os endpoints de auth do backend (`backend/src/Rateio.Api/Controllers/AuthController.cs`):
 * login via Google (T11) e logout (T14.1). O client HTTP inteiro (ver
 * [com.rateio.data.remote.RateioHttpClientFactory]) existe só pra estes dois.
 */
interface AuthApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): GoogleLoginResponseDto

    /**
     * Espelha `POST /auth/logout` (T14.1). Sem corpo de resposta (204) — só lança se a chamada de
     * rede falhar, que é o único caso que `RemoteAuthRepository.signOut` trata (logout local
     * segue mesmo assim).
     */
    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto)
}

/** Corpo de `POST /auth/google` — espelha `GoogleLoginRequest` (record `IdToken`) do backend. */
@Serializable
data class GoogleLoginRequestDto(val idToken: String)

/** Resposta de sucesso — espelha `GoogleLoginResponse` do backend. */
@Serializable
data class GoogleLoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

/** Espelha `UsuarioResponse(Nome, Email)` do backend (System.Text.Json em camelCase). */
@Serializable
data class UserDto(val nome: String, val email: String)

/**
 * Corpo de `POST /auth/logout` — espelha `LogoutRequest` (record `RefreshToken`) do backend. É o
 * refresh token, não o access token, porque é ele que identifica a sessão a revogar (mesma
 * justificativa do contrato do backend).
 */
@Serializable
data class LogoutRequestDto(val refreshToken: String)
