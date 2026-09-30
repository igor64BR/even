package com.tally.data.remote.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Mirrors the backend's auth endpoints (`backend/src/Tally.Api/Controllers/AuthController.cs`):
 * login via Google and logout. The whole HTTP client (see
 * [com.tally.data.remote.TallyHttpClientFactory]) exists just for these two.
 */
interface AuthApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): GoogleLoginResponseDto

    /**
     * Mirrors `POST /auth/logout`. No response body (204) — only throws if the network
     * call fails, the only case `RemoteAuthRepository.signOut` handles (local logout proceeds
     * anyway).
     */
    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto)
}

/** Body of `POST /auth/google` — mirrors the backend's `GoogleLoginRequest` (record `IdToken`). */
@Serializable
data class GoogleLoginRequestDto(val idToken: String)

/** Success response — mirrors the backend's `GoogleLoginResponse`. */
@Serializable
data class GoogleLoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

/** Mirrors the backend's `UserResponse(Name, Email)` (System.Text.Json in camelCase). */
@Serializable
data class UserDto(val name: String, val email: String)

/**
 * Body of `POST /auth/logout` — mirrors the backend's `LogoutRequest` (record `RefreshToken`). It's
 * the refresh token, not the access token, because that's what identifies the session to revoke
 * (same rationale as the backend contract).
 */
@Serializable
data class LogoutRequestDto(val refreshToken: String)
