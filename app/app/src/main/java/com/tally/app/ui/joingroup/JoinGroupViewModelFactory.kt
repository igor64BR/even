package com.tally.app.ui.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tally.domain.repository.AuthRepository
import com.tally.domain.repository.RemoteGroupRepository

/**
 * No DI framework in the project yet — manual factory that injects the repositories from
 * [com.tally.app.di.AppContainer] into [JoinGroupViewModel]. Same pattern as
 * [com.tally.app.ui.auth.AuthViewModelFactory], with [inviteCode] because the screen always
 * belongs to a specific invite (code extracted from the deep link).
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
