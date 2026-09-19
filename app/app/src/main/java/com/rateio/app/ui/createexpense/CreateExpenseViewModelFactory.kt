package com.rateio.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.ParticipantRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [CreateExpenseViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.creategroup.CreateGroupViewModelFactory] (T16), com [groupId] adicional
 * porque, diferente de "Novo grupo", este formulário sempre pertence a um grupo já existente.
 */
class CreateExpenseViewModelFactory(
    private val groupId: String,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(CreateExpenseViewModel::class.java)) {
            "CreateExpenseViewModelFactory só sabe criar CreateExpenseViewModel, pediram $modelClass"
        }
        return CreateExpenseViewModel(groupId, participantRepository, expenseRepository) as T
    }
}
