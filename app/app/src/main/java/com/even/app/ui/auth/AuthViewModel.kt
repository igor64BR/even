package com.even.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.even.app.auth.GoogleIdentityClient
import com.even.app.auth.GoogleIdentityResult
import com.even.domain.model.AuthSession
import com.even.domain.repository.AuthRepository
import com.even.domain.repository.AuthenticationFailedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State of the login/account screen. The source of truth for "is signed in?" is
 * [AuthRepository.getSessionFlow] (persisted via `EncryptedSharedPreferences`) — [phase]
 * only covers the transient states that session alone doesn't model (connecting, an error from
 * the last attempt). Same MVVM pattern as [com.even.app.ui.groups.GroupListViewModel]:
 * `combine` + `stateIn`, no network logic in the Composable.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleIdentityClient: GoogleIdentityClient,
) : ViewModel() {

    private val phase = MutableStateFlow<Phase>(Phase.Idle)

    val uiState: StateFlow<AuthUiState> = combine(authRepository.getSessionFlow(), phase) { session, phase ->
        session.toUiState(phase)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AuthUiState.SignedOut(),
    )

    fun signInWithGoogle() {
        viewModelScope.launch {
            phase.value = Phase.Connecting
            when (val identity = googleIdentityClient.requestGoogleIdToken()) {
                is GoogleIdentityResult.Failure -> phase.value = Phase.Error(identity.reason)
                is GoogleIdentityResult.Success -> exchangeForSession(identity.idToken)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            phase.value = Phase.Idle
        }
    }

    private suspend fun exchangeForSession(googleIdToken: String) {
        try {
            authRepository.signInWithGoogle(googleIdToken)
            phase.value = Phase.Idle
        } catch (error: AuthenticationFailedException) {
            phase.value = Phase.Error(error.message ?: DEFAULT_ERROR_MESSAGE)
        }
    }

    private fun AuthSession?.toUiState(phase: Phase): AuthUiState {
        if (this != null) return AuthUiState.SignedIn(user)
        return phase.toSignedOutUiState()
    }

    private fun Phase.toSignedOutUiState(): AuthUiState = when (this) {
        Phase.Idle -> AuthUiState.SignedOut()
        Phase.Connecting -> AuthUiState.Connecting
        is Phase.Error -> AuthUiState.SignedOut(errorMessage = reason)
    }

    /** Transient UI states that don't come from the persisted [AuthSession]. */
    private sealed interface Phase {
        data object Idle : Phase
        data object Connecting : Phase
        data class Error(val reason: String) : Phase
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_ERROR_MESSAGE = "Couldn't sign in with that account."
    }
}
