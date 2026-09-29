package com.rateio.app.ui.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.engine.DebtSimplificationEngine
import com.rateio.domain.realtime.GroupRealtimeGateway
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteExpenseRepository
import com.rateio.domain.repository.RemoteGroupRepository
import com.rateio.domain.repository.SettlementRepository

/**
 * No DI framework in the project yet — a manual factory that injects
 * [com.rateio.app.di.AppContainer]'s repositories into the [GroupDetailViewModel]. Same pattern as
 * [com.rateio.app.ui.createexpense.CreateExpenseViewModelFactory] (T24), with a [groupId] because
 * the screen always belongs to an already-existing group. T40.1 adds [groupRealtimeGateway] for
 * the SignalR client.
 */
class GroupDetailViewModelFactory(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
    private val remoteExpenseRepository: RemoteExpenseRepository,
    private val debtSimplificationEngine: DebtSimplificationEngine,
    private val groupRealtimeGateway: GroupRealtimeGateway,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            "GroupDetailViewModelFactory only knows how to create GroupDetailViewModel, got $modelClass"
        }
        return GroupDetailViewModel(
            groupId = groupId,
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            settlementRepository = settlementRepository,
            authRepository = authRepository,
            remoteGroupRepository = remoteGroupRepository,
            remoteExpenseRepository = remoteExpenseRepository,
            debtSimplificationEngine = debtSimplificationEngine,
            groupRealtimeGateway = groupRealtimeGateway,
        ) as T
    }
}
