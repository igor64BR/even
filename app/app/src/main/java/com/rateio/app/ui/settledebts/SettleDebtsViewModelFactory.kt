package com.rateio.app.ui.settledebts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.engine.DebtSimplificationEngine
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.SettlementRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [SettleDebtsViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.groupdetail.GroupDetailViewModelFactory].
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
            "SettleDebtsViewModelFactory só sabe criar SettleDebtsViewModel, pediram $modelClass"
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
