package com.even.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.even.domain.repository.AuthRepository
import com.even.domain.repository.NotificationRepository

/**
 * No DI framework in the project yet — manual factory that injects the repositories from
 * [com.even.app.di.AppContainer] into [NotificationsViewModel]. Same pattern as the other
 * screens.
 */
class NotificationsViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            "NotificationsViewModelFactory only knows how to create NotificationsViewModel, got $modelClass"
        }
        return NotificationsViewModel(
            notificationRepository = notificationRepository,
            authRepository = authRepository,
        ) as T
    }
}
