package com.tally.domain.repository

import com.tally.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

/**
 * Contract for optional authentication (constitution.md, principles 1 and 2). `:domain` only sees
 * "a Google ID token goes in, an [AuthSession] comes out" — Credential Manager, Retrofit and
 * EncryptedSharedPreferences are implementation details of `:data`/`:app` (Dependency Inversion),
 * none of those types leak into this interface.
 */
interface AuthRepository {

    /** `null` while no one has authenticated on this device yet — the normal state, not an error. */
    fun getSessionFlow(): Flow<AuthSession?>

    /**
     * Exchanges a Google ID token (already obtained via Credential Manager) for the backend's own
     * JWT (`POST /auth/google`, T11) and persists the resulting session.
     *
     * @throws AuthenticationFailedException if the backend rejects the ID token or the call fails.
     */
    suspend fun signInWithGoogle(googleIdToken: String): AuthSession

    /** Tears down the local session. Local groups aren't affected (local-first, principle 1). */
    suspend fun signOut()
}

/**
 * An authentication failure already translated into a presentable message — [AuthRepository]
 * never lets a network (Retrofit/OkHttp) or parsing exception leak out to the caller.
 */
class AuthenticationFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)
