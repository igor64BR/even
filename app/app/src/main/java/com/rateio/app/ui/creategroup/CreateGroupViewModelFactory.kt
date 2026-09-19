package com.rateio.app.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [CreateGroupViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.groups.GroupListViewModelFactory] (T8).
 */
class CreateGroupViewModelFactory(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(CreateGroupViewModel::class.java)) {
            "CreateGroupViewModelFactory só sabe criar CreateGroupViewModel, pediram $modelClass"
        }
        return CreateGroupViewModel(groupRepository, participantRepository) as T
    }
}
