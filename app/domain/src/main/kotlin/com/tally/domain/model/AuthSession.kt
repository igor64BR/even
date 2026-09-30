package com.tally.domain.model

/**
 * Authenticated sync session: a pair of tokens from the backend's own JWT — a short-lived
 * `accessToken`, a revocable `refreshToken` — plus the [user] they represent. Login is optional —
 * the absence of an [AuthSession] is the app's normal state, not an error;
 * [com.tally.domain.repository.AuthRepository.getSessionFlow] models this as `AuthSession?`,
 * never throwing an exception for "not signed in".
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthenticatedUser,
)
