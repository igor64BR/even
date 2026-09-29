package com.tally.app.ui.notifications

/**
 * State for the "Notifications" screen (T41.1), faithful to `prototype/notifications.html`:
 * loading, "requires account" (user with no session — local groups have no one to notify), empty
 * list, or content.
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data object RequiresAccount : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Content(val notifications: List<NotificationRowUiModel>) : NotificationsUiState
}

/** A prototype `.notif-row` — ready-to-show text, relative time, and whether it's already read. */
data class NotificationRowUiModel(
    val id: String,
    val message: String,
    val relativeTime: String,
    val isRead: Boolean,
)
