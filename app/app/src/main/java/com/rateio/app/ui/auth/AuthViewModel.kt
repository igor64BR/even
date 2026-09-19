package com.rateio.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.app.auth.GoogleIdentityClient
import com.rateio.app.auth.GoogleIdentityResult
import com.rateio.domain.model.AuthSession
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.AuthenticationFailedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela de login/conta (T12). Fonte de verdade de "está logado?" é
 * [AuthRepository.getSessionFlow] (T12.2, persistida via `EncryptedSharedPreferences`) — [phase]
 * só cobre os estados transitórios que essa sessão sozinha não modela (conectando, erro da
 * última tentativa). Mesmo padrão MVVM de [com.rateio.app.ui.groups.GroupListViewModel] (T8):
 * `combine` + `stateIn`, nenhuma lógica de rede na Composable.
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

    /** Estados transitórios de UI que não vêm da [AuthSession] persistida. */
    private sealed interface Phase {
        data object Idle : Phase
        data object Connecting : Phase
        data class Error(val reason: String) : Phase
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_ERROR_MESSAGE = "Não foi possível entrar com essa conta."
    }
}
