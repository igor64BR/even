package com.rateio.domain.model

/**
 * Sessão autenticada de sincronização: par de tokens do JWT próprio do backend (T11 —
 * `accessToken` de vida curta, `refreshToken` revogável) mais o [user] que eles representam.
 * Login é opcional (constitution.md, princípio 1) — a ausência de [AuthSession] é o estado normal
 * do app, não um erro; [com.rateio.domain.repository.AuthRepository.getSessionFlow] modela isso
 * como `AuthSession?`, nunca lança exceção por "não estar logado".
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthenticatedUser,
)
