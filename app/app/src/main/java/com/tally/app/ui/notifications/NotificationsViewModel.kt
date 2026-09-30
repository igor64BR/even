package com.tally.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.ui.format.formatInstantAsRelative
import com.tally.domain.model.GroupNotification
import com.tally.domain.repository.AuthRepository
import com.tally.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the "Notifications" screen. The source is [NotificationRepository]
 * (Room) — "requires account" (RequiresAccount) reacts directly to
 * [AuthRepository.getSessionFlow], without waiting for the list: an unsynced local group has no
 * one to notify (same reasoning as the prototype's `notifications.html`).
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

    /** Badge for the "Notifications" tab itself while this screen is open — same source [com.tally.app.ui.groups.GroupListViewModel] uses for the badge on "Your groups". */
    val unreadNotificationsCount: StateFlow<Int> = notificationRepository.getUnreadCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    /**
     * Faithful to the prototype (`notifications.html`, `render()`): opening the screen shows the
     * real read/unread state for the current visit, and only marks everything as read when the
     * screen is closed — that way the badge and a new visit already start at zero, but
     * this visit doesn't have the unread dot "flicker" away under the user (unlike the static
     * prototype, which only re-renders once; here the state is reactive via Flow, so marking as
     * read before leaving would make the dot disappear from the screen while the user is still
     * looking at it).
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
