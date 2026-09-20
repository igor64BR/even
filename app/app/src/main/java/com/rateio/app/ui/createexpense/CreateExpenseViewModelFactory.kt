package com.rateio.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteExpenseRepository

/**
 * Sem framework de DI no projeto ainda — fábrica manual que injeta os repositórios de
 * [com.rateio.app.di.AppContainer] no [CreateExpenseViewModel]. Mesmo padrão de
 * [com.rateio.app.ui.creategroup.CreateGroupViewModelFactory] (T16), com [groupId] adicional
 * porque, diferente de "Novo grupo", este formulário sempre pertence a um grupo já existente.
 *
 * [expenseId] (T29) é `null` pra "Nova despesa" e o id da despesa sendo editada pra "Editar
 * despesa" — [groupRepository]/[remoteExpenseRepository] só são efetivamente usados no modo
 * edição (propagar a mudança pro backend quando o grupo já está sincronizado), mas entram na
 * fábrica pros dois modos porque é o mesmo [CreateExpenseViewModel] pros dois (T29, "edição é
 * estado, não tela nova").
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
            "CreateExpenseViewModelFactory só sabe criar CreateExpenseViewModel, pediram $modelClass"
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
