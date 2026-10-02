package com.even.app.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.ParticipantRepository

/**
 * No DI framework in the project yet — a manual factory that injects
 * [com.even.app.di.AppContainer]'s repositories into the [CreateGroupViewModel]. Same pattern as
 * [com.even.app.ui.groups.GroupListViewModelFactory].
 */
class CreateGroupViewModelFactory(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(CreateGroupViewModel::class.java)) {
            "CreateGroupViewModelFactory only knows how to create CreateGroupViewModel, got $modelClass"
        }
        return CreateGroupViewModel(groupRepository, participantRepository) as T
    }
}
