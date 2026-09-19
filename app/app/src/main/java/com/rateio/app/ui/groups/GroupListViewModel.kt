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
 * Lacuna conhecida por T8 (que reportou em vez de inventar dado, já que estava restrita a
 * `app/app/`): faltava sinalizador de sincronização em `Group` e divisão por participante em
 * `Expense`. T7B (`:domain`/`:data`) resolveu a primeira metade — [Group.isSynced] agora é campo
 * real, e [toUiModel] usa o valor de verdade em vez do `false` fixo que T8 tinha colocado como
 * placeholder. [GroupListItemUiModel.balance] continua [GroupBalance.Settled] fixo: calcular saldo
 * de verdade depende do motor de simplificação em Kotlin (RF25-RF28, T33), que ainda não existe —
 * `Expense.splits` (T7B.3) já modela os dados que T33 vai consumir, mas rodar o algoritmo é escopo
 * daquela task, não desta.
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
    isSynced = isSynced,
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
