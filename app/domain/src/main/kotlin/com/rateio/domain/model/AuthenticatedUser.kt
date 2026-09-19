package com.rateio.domain.model

/**
 * Usuário autenticado via Google (constitution.md, princípio 2 — única forma de autenticação,
 * sem senha própria). Espelha `UsuarioResponse` de `POST /auth/google` (T11); só os dois campos
 * que a resposta do backend carrega, nada de token aqui — isso é [AuthSession].
 */
data class AuthenticatedUser(
    val name: String,
    val email: String,
)
