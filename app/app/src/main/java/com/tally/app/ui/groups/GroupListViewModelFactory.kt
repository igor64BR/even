package com.tally.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.NotificationRepository
import com.tally.domain.repository.ParticipantRepository

/**
 * No DI framework in the project yet — manual factory that injects the repositories from
 * [com.tally.app.di.AppContainer] into [GroupListViewModel]. Up until T19 it also received
 * `authRepository`/`remoteGroupRepository`/`expenseRepository` for the "Sync this group" action;
 * T42.4 moves that action to [com.tally.app.ui.groupdetail.GroupDetailViewModelFactory]. T41.2
 * adds [notificationRepository] for the "Notifications" tab badge.
 */
class GroupListViewModelFactory(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(GroupListViewModel::class.java)) {
            "GroupListViewModelFactory only knows how to create GroupListViewModel, got $modelClass"
        }
        return GroupListViewModel(
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            notificationRepository = notificationRepository,
        ) as T
    }
}
