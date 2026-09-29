package com.rateio.app.ui.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.RemoteGroupRepository

/**
 * No DI framework in the project yet — manual factory that injects the repositories from
 * [com.rateio.app.di.AppContainer] into [JoinGroupViewModel]. Same pattern as
 * [com.rateio.app.ui.auth.AuthViewModelFactory] (T12), with [inviteCode] because the screen always
 * belongs to a specific invite (code extracted from the deep link, T22.1).
 */
class JoinGroupViewModelFactory(
    private val inviteCode: String,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(JoinGroupViewModel::class.java)) {
            "JoinGroupViewModelFactory only knows how to create JoinGroupViewModel, got $modelClass"
        }
        return JoinGroupViewModel(inviteCode, authRepository, remoteGroupRepository) as T
    }
}
