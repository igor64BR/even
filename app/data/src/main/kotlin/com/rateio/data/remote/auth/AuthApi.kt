package com.rateio.data.remote.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Espelha `POST /auth/google` (T11 — `backend/src/Rateio.Api/Controllers/AuthController.cs`).
 * Único endpoint do backend consumido nesta task; o client HTTP inteiro (ver
 * [com.rateio.data.remote.RateioHttpClientFactory]) existe só pra ele.
 */
interface AuthApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): GoogleLoginResponseDto
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
