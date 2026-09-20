package com.rateio.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.NotificationRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [NotificationsViewModel]. Mesmo padrão das demais telas.
 */
class NotificationsViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            "NotificationsViewModelFactory só sabe criar NotificationsViewModel, pediram $modelClass"
        }
        return NotificationsViewModel(
            notificationRepository = notificationRepository,
            authRepository = authRepository,
        ) as T
    }
}
