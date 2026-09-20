package com.rateio.app.ui.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado do fluxo "entrar no grupo por link" (T22.1/T22.2). Fonte de verdade de "está logado?" é
 * [AuthRepository.getSessionFlow], mesmo padrão MVVM de [com.rateio.app.ui.auth.AuthViewModel]
 * (T12) e [com.rateio.app.ui.groupdetail.GroupDetailViewModel] (T19/T42.4): `combine` + `stateIn`,
 * nenhuma lógica de rede na Composable.
 *
 * Este ViewModel só decide *o que mostrar*; ele não navega sozinho. Quando a sessão está ausente,
 * [uiState] vira [JoinGroupUiState.NeedsLogin] e quem observa (`JoinGroupRoute`) é responsável por
 * navegar pra tela de login e voltar pra cá depois — a mesma instância de [inviteCode] é reusada
 * pra retomar (T22, "guarde a intenção... após login bem-sucedido, retome o fluxo automaticamente").
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

    /** Botão "Entrar" da tela de confirmação. Ignorado se já houver uma chamada em andamento. */
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

    /** "Tentar de novo" após [JoinGroupUiState.Error] — volta pra confirmação, não refaz a chamada sozinho. */
    fun retry() {
        phase.value = Phase.Confirming
    }

    private fun Phase.toUiState(inviteCode: String): JoinGroupUiState = when (this) {
        Phase.Confirming -> JoinGroupUiState.Confirming(inviteCode)
        Phase.Joining -> JoinGroupUiState.Joining
        is Phase.Success -> JoinGroupUiState.Success(remoteGroupId)
        is Phase.Error -> JoinGroupUiState.Error(inviteCode, message)
    }

    /** Estados transitórios que não vêm da sessão persistida — mesma ideia de `AuthViewModel.Phase`. */
    private sealed interface Phase {
        data object Confirming : Phase
        data object Joining : Phase
        data class Success(val remoteGroupId: String) : Phase
        data class Error(val message: String) : Phase
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_ERROR_MESSAGE = "Não foi possível entrar nesse grupo."
    }
}
