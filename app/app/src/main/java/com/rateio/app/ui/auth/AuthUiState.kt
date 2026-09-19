package com.rateio.app.ui.auth

import com.rateio.domain.model.AuthenticatedUser

/**
 * Estado da tela de login/conta (T12.1) — espelha os estados de `prototype/login.html`:
 * deslogado (com ou sem erro da última tentativa), conectando (overlay "Conectando ao Google…")
 * e logado (bloco "Sua conta").
 */
sealed interface AuthUiState {
    data class SignedOut(val errorMessage: String? = null) : AuthUiState
    data object Connecting : AuthUiState
    data class SignedIn(val user: AuthenticatedUser) : AuthUiState
}
