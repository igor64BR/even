package com.tally.app.ui.auth

import com.tally.domain.model.AuthenticatedUser

/**
 * State of the login/account screen — mirrors `prototype/login.html`'s states: signed
 * out (with or without an error from the last attempt), connecting (the "Connecting to Google…"
 * overlay) and signed in (the "Your account" block).
 */
sealed interface AuthUiState {
    data class SignedOut(val errorMessage: String? = null) : AuthUiState
    data object Connecting : AuthUiState
    data class SignedIn(val user: AuthenticatedUser) : AuthUiState
}
