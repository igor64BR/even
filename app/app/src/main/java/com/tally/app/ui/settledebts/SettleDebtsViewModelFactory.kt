package com.tally.app.ui.settledebts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tally.domain.engine.DebtSimplificationEngine
import com.tally.domain.repository.ExpenseRepository
import com.tally.domain.repository.GroupRepository
import com.tally.domain.repository.ParticipantRepository
import com.tally.domain.repository.SettlementRepository

/**
 * No DI framework in the project yet — manual factory that injects the repositories from
 * [com.tally.app.di.AppContainer] into [SettleDebtsViewModel]. Same pattern as
 * [com.tally.app.ui.groupdetail.GroupDetailViewModelFactory].
 */
class SettleDebtsViewModelFactory(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val debtSimplificationEngine: DebtSimplificationEngine,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(SettleDebtsViewModel::class.java)) {
            "SettleDebtsViewModelFactory only knows how to create SettleDebtsViewModel, got $modelClass"
        }
        return SettleDebtsViewModel(
            groupId = groupId,
            groupRepository = groupRepository,
            participantRepository = participantRepository,
            expenseRepository = expenseRepository,
            settlementRepository = settlementRepository,
            debtSimplificationEngine = debtSimplificationEngine,
        ) as T
    }
}
