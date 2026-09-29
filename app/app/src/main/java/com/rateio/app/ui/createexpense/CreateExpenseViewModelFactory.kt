package com.rateio.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteExpenseRepository

/**
 * No DI framework in the project yet — a manual factory that injects
 * [com.rateio.app.di.AppContainer]'s repositories into the [CreateExpenseViewModel]. Same pattern
 * as [com.rateio.app.ui.creategroup.CreateGroupViewModelFactory] (T16), with an additional
 * [groupId] because, unlike "New group", this form always belongs to an already-existing group.
 *
 * [expenseId] (T29) is `null` for "New expense" and the id of the expense being edited for "Edit
 * expense" — [groupRepository]/[remoteExpenseRepository] are only actually used in edit mode
 * (propagating the change to the backend when the group is already synced), but they're part of
 * the factory for both modes because it's the same [CreateExpenseViewModel] for both (T29,
 * "editing is state, not a new screen").
 */
class CreateExpenseViewModelFactory(
    private val groupId: String,
    private val expenseId: String? = null,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val remoteExpenseRepository: RemoteExpenseRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(CreateExpenseViewModel::class.java)) {
            "CreateExpenseViewModelFactory only knows how to create CreateExpenseViewModel, got $modelClass"
        }
        return CreateExpenseViewModel(
            groupId = groupId,
            expenseId = expenseId,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            groupRepository = groupRepository,
            remoteExpenseRepository = remoteExpenseRepository,
        ) as T
    }
}
