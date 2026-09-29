package com.tally.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tally.app.auth.GoogleIdentityClient
import com.tally.domain.repository.AuthRepository

/**
 * No DI framework in the project yet — a manual factory that injects [AuthRepository] (from
 * [com.tally.app.di.AppContainer]) and [GoogleIdentityClient] into the [AuthViewModel]. Same
 * convention as [com.tally.app.ui.groups.GroupListViewModelFactory] (T8).
 */
class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val googleIdentityClient: GoogleIdentityClient,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "AuthViewModelFactory only knows how to create AuthViewModel, got $modelClass"
        }
        return AuthViewModel(authRepository, googleIdentityClient) as T
    }
}
