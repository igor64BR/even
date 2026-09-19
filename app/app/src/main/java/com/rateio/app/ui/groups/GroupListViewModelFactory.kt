package com.rateio.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteGroupRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [GroupListViewModel]. [authRepository]/[remoteGroupRepository]
 * entraram em T19 pra ação "Sincronizar este grupo".
 */
class GroupListViewModelFactory(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(GroupListViewModel::class.java)) {
            "GroupListViewModelFactory só sabe criar GroupListViewModel, pediram $modelClass"
        }
        return GroupListViewModel(
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            authRepository = authRepository,
            remoteGroupRepository = remoteGroupRepository,
        ) as T
    }
}
