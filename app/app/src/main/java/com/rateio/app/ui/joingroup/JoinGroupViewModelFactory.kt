package com.rateio.app.ui.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.RemoteGroupRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [JoinGroupViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.auth.AuthViewModelFactory] (T12), com [inviteCode] porque a tela sempre
 * pertence a um convite específico (código extraído do deep link, T22.1).
 */
class JoinGroupViewModelFactory(
    private val inviteCode: String,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(JoinGroupViewModel::class.java)) {
            "JoinGroupViewModelFactory só sabe criar JoinGroupViewModel, pediram $modelClass"
        }
        return JoinGroupViewModel(inviteCode, authRepository, remoteGroupRepository) as T
    }
}
