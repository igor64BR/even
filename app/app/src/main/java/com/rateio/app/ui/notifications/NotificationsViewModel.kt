package com.rateio.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.app.ui.format.formatInstantAsRelative
import com.rateio.domain.model.GroupNotification
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado da tela "Notificações" (T41.1, RF35/RF36). Fonte é [NotificationRepository] (Room, T40.1)
 * — "exige conta" (RequiresAccount) reage a [AuthRepository.getSessionFlow] direto, sem esperar a
 * lista: grupo local não sincronizado não tem pra quem avisar (mesmo racional do protótipo,
 * `notificacoes.html`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = authRepository.getSessionFlow()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(NotificationsUiState.RequiresAccount)
            } else {
                notificationRepository.getNotificationsFlow().map { it.toUiState() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = NotificationsUiState.Loading,
        )

    /** Badge da própria aba "Avisos" (T41.2) enquanto esta tela está aberta — mesma fonte que [com.rateio.app.ui.groups.GroupListViewModel] usa pro badge em "Seus grupos". */
    val unreadNotificationsCount: StateFlow<Int> = notificationRepository.getUnreadCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    /**
     * Fiel ao protótipo (`notificacoes.html`, `render()`): abrir a tela mostra o estado real de
     * lida/não lida da visita atual, e só marca tudo como lido quando a tela é fechada — assim o
     * badge (T41.2) e uma nova visita já saem zerados, mas esta visita não "pisca" o ponto de não
     * lida sumindo por baixo do usuário (diferente do protótipo estático, que re-renderiza só uma
     * vez; aqui o estado é reativo via Flow, então marcar como lido antes de sair faria o ponto
     * desaparecer da tela ainda com o usuário olhando).
     */
    fun onScreenClosed() {
        viewModelScope.launch { notificationRepository.markAllAsRead() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun List<GroupNotification>.toUiState(): NotificationsUiState {
    if (isEmpty()) return NotificationsUiState.Empty
    return NotificationsUiState.Content(map { it.toRowUiModel() })
}

private fun GroupNotification.toRowUiModel() = NotificationRowUiModel(
    id = id,
    message = message,
    relativeTime = formatInstantAsRelative(occurredAt),
    isRead = isRead,
)
