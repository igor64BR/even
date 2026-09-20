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
import com.rateio.domain.repository.RemoteGroupRepository
import com.rateio.domain.repository.SettlementRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [GroupDetailViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.createexpense.CreateExpenseViewModelFactory] (T24), com [groupId] porque a
 * tela sempre pertence a um grupo já existente. T40.1 acrescenta [groupRealtimeGateway] pro
 * cliente SignalR.
 */
class GroupDetailViewModelFactory(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
    private val debtSimplificationEngine: DebtSimplificationEngine,
    private val groupRealtimeGateway: GroupRealtimeGateway,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            "GroupDetailViewModelFactory só sabe criar GroupDetailViewModel, pediram $modelClass"
        }
        return GroupDetailViewModel(
            groupId = groupId,
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            settlementRepository = settlementRepository,
            authRepository = authRepository,
            remoteGroupRepository = remoteGroupRepository,
            debtSimplificationEngine = debtSimplificationEngine,
            groupRealtimeGateway = groupRealtimeGateway,
        ) as T
    }
}
