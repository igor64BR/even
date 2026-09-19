package com.rateio.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.app.auth.GoogleIdentityClient
import com.rateio.domain.repository.AuthRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta [AuthRepository] (de
 * [com.rateio.app.di.AppContainer]) e [GoogleIdentityClient] no [AuthViewModel]. Mesma convenção
 * de [com.rateio.app.ui.groups.GroupListViewModelFactory] (T8).
 */
class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val googleIdentityClient: GoogleIdentityClient,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "AuthViewModelFactory só sabe criar AuthViewModel, pediram $modelClass"
        }
        return AuthViewModel(authRepository, googleIdentityClient) as T
    }
}
