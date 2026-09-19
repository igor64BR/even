package com.rateio.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.model.Group
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado da tela "Seus grupos" (RF40/RF41). Fonte de dados é [GroupRepository] +
 * [ParticipantRepository] — as duas portas de `:domain` que `:data` já implementa sobre Room
 * (T7); zero chamada de rede, como pede T8.
 *
 * Lacuna conhecida, reportada em vez de inventada (T8 pede exatamente isso quando falta algo em
 * `:domain`/`:data`): nem [Group] nem `Expense` carregam hoje o que o card completo do protótipo
 * precisa — falta um sinalizador de sincronização em `Group` e a divisão por participante
 * (`divisao`) em `Expense`, sem a qual não dá para calcular saldo real (o motor de
 * simplificação, RF25-RF28, ainda não tem porta Kotlin — ver plan.md). Como T8 restringe o
 * escopo a `app/app/` (não pode alterar `:domain`/`:data`), [GroupListItemUiModel.isSynced] e
 * [GroupListItemUiModel.balance] usam o valor neutro mais honesto disponível — local/quitado —
 * em vez de um cálculo inventado. Uma task futura de modelagem (sinalizador de sync + divisão de
 * despesa + motor Kotlin) deve substituir [toUiModel] por dados reais.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModel(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> = groupRepository.getGroupsFlow()
        .flatMapLatest { groups -> observeContent(groups) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GroupListUiState.Loading,
        )

    private fun observeContent(groups: List<Group>): Flow<GroupListUiState> {
        if (groups.isEmpty()) return flowOf(GroupListUiState.Empty)

        val itemFlows = groups.map { group ->
            participantRepository.getParticipantsFlow(group.id)
                .map { participants -> group.toUiModel(participantCount = participants.size) }
        }
        return combine(itemFlows) { items -> GroupListUiState.Content(items.toList()) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Group.toUiModel(participantCount: Int) = GroupListItemUiModel(
    id = id,
    name = name,
    tag = tagFor(name),
    participantCount = participantCount,
    isSynced = false,
    balance = GroupBalance.Settled,
)

/**
 * Sigla de duas letras para o `group-tag` do card — mesma heurística de `index.html`: primeira
 * palavra "significativa" (mais de 2 letras ou iniciando maiúscula), com fallback pro nome
 * inteiro.
 */
private fun tagFor(name: String): String {
    val significantWord = name.split(" ")
        .firstOrNull { word -> word.length > 2 || word.firstOrNull()?.isUpperCase() == true }
    return (significantWord ?: name).take(2).uppercase()
}
