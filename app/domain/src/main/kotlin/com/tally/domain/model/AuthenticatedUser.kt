package com.tally.domain.model

/**
 * User authenticated via Google — the only authentication method, no self-managed password.
 * Mirrors `UserResponse` from `POST /auth/google`; just the two fields the backend response
 * carries, no token here — that's [AuthSession].
 */
data class AuthenticatedUser(
    val name: String,
    val email: String,
)
