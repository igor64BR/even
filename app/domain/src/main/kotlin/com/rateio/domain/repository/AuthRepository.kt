package com.rateio.domain.repository

import com.rateio.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticação opcional (constitution.md, princípio 1 e 2). `:domain` só enxerga "um
 * ID token do Google entra, uma [AuthSession] sai" — Credential Manager, Retrofit e
 * EncryptedSharedPreferences são detalhe de implementação de `:data`/`:app` (Dependency
 * Inversion), nenhum desses tipos vaza para esta interface.
 */
interface AuthRepository {

    /** `null` enquanto ninguém autenticou neste aparelho — estado normal, não um erro. */
    fun getSessionFlow(): Flow<AuthSession?>

    /**
     * Troca um ID token do Google (já obtido via Credential Manager) pelo JWT próprio do backend
     * (`POST /auth/google`, T11) e persiste a sessão resultante.
     *
     * @throws AuthenticationFailedException se o backend rejeitar o ID token ou a chamada falhar.
     */
    suspend fun signInWithGoogle(googleIdToken: String): AuthSession

    /** Derruba a sessão local. Grupos locais não são afetados (local-first, princípio 1). */
    suspend fun signOut()
}

/**
 * Falha de autenticação já traduzida para uma mensagem apresentável — [AuthRepository] nunca deixa
 * uma exceção de rede (Retrofit/OkHttp) ou de parsing vazar para quem chama.
 */
class AuthenticationFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)
