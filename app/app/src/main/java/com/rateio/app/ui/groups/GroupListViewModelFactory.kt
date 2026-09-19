package com.rateio.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [GroupListViewModel]. Até T19 recebia também
 * `authRepository`/`remoteGroupRepository`/`expenseRepository` pra ação "Sincronizar este grupo";
 * T42.4 move essa ação pra [com.rateio.app.ui.groupdetail.GroupDetailViewModelFactory], então a
 * lista volta a depender só do que ela de fato mostra.
 */
class GroupListViewModelFactory(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(GroupListViewModel::class.java)) {
            "GroupListViewModelFactory só sabe criar GroupListViewModel, pediram $modelClass"
        }
        return GroupListViewModel(
            groupRepository = groupRepository,
            participantRepository = participantRepository,
        ) as T
    }
}
