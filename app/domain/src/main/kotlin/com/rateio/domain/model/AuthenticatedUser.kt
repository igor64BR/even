package com.rateio.domain.model

/**
 * User authenticated via Google (constitution.md, principle 2 — the only authentication method,
 * no self-managed password). Mirrors `UserResponse` from `POST /auth/google` (T11); just the two
 * fields the backend response carries, no token here — that's [AuthSession].
 */
data class AuthenticatedUser(
    val name: String,
    val email: String,
)
