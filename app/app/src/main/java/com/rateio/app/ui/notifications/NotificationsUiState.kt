package com.rateio.app.ui.notifications

/**
 * Estado da tela "Notificações" (T41.1), fiel a `prototype/notificacoes.html`: carregando, "exige
 * conta" (usuário sem sessão — grupos locais não têm pra quem avisar), lista vazia, ou conteúdo.
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data object RequiresAccount : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Content(val notifications: List<NotificationRowUiModel>) : NotificationsUiState
}

/** Uma linha `.notif-row` do protótipo — texto pronto, horário relativo, e se já foi lida. */
data class NotificationRowUiModel(
    val id: String,
    val message: String,
    val relativeTime: String,
    val isRead: Boolean,
)
