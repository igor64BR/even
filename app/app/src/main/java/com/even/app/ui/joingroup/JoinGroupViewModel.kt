package com.even.app.ui.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.even.domain.repository.AuthRepository
import com.even.domain.repository.GroupSyncException
import com.even.domain.repository.RemoteGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the "join group via link" flow. The source of truth for "is signed in?"
 * is [AuthRepository.getSessionFlow], same MVVM pattern as [com.even.app.ui.auth.AuthViewModel]
 * and [com.even.app.ui.groupdetail.GroupDetailViewModel]: `combine` + `stateIn`,
 * no network logic in the Composable.
 *
 * This ViewModel only decides *what to show*; it never navigates on its own. When there's no
 * session, [uiState] becomes [JoinGroupUiState.NeedsLogin] and whoever observes it (`JoinGroupRoute`)
 * is responsible for navigating to the login screen and coming back here afterwards — the same
 * [inviteCode] instance is reused to resume the flow automatically after a successful login.
 */
class JoinGroupViewModel(
    private val inviteCode: String,
    authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
) : ViewModel() {

    private val phase = MutableStateFlow<Phase>(Phase.Confirming)

    val uiState: StateFlow<JoinGroupUiState> = combine(authRepository.getSessionFlow(), phase) { session, phase ->
        if (session == null) JoinGroupUiState.NeedsLogin(inviteCode) else phase.toUiState(inviteCode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = JoinGroupUiState.CheckingSession,
    )

    /** "Join" button on the confirmation screen. Ignored if a call is already in progress. */
    fun confirm() {
        if (phase.value is Phase.Joining) return

        viewModelScope.launch {
            phase.value = Phase.Joining
            try {
                val remoteGroupId = remoteGroupRepository.joinByCode(inviteCode)
                phase.value = Phase.Success(remoteGroupId)
            } catch (error: GroupSyncException) {
                phase.value = Phase.Error(error.message ?: DEFAULT_ERROR_MESSAGE)
            }
        }
    }

    /** "Try again" after [JoinGroupUiState.Error] — goes back to confirmation, doesn't redo the call by itself. */
    fun retry() {
        phase.value = Phase.Confirming
    }

    private fun Phase.toUiState(inviteCode: String): JoinGroupUiState = when (this) {
        Phase.Confirming -> JoinGroupUiState.Confirming(inviteCode)
        Phase.Joining -> JoinGroupUiState.Joining
        is Phase.Success -> JoinGroupUiState.Success(remoteGroupId)
        is Phase.Error -> JoinGroupUiState.Error(inviteCode, message)
    }

    /** Transient states that don't come from the persisted session — same idea as `AuthViewModel.Phase`. */
    private sealed interface Phase {
        data object Confirming : Phase
        data object Joining : Phase
        data class Success(val remoteGroupId: String) : Phase
        data class Error(val message: String) : Phase
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_ERROR_MESSAGE = "Could not join this group."
    }
}
