package com.rateio.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.model.Group
import com.rateio.domain.repository.AuthRepository
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteGroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela "Seus grupos" (RF40/RF41). Fonte de dados é [GroupRepository] +
 * [ParticipantRepository] — as duas portas de `:domain` que `:data` já implementa sobre Room
 * (T7); zero chamada de rede pra montar a lista, como pede T8. [AuthRepository] entra só pra
 * decidir se a ação "Sincronizar" (T19) aparece — sincronizar é a única operação daqui que fala
 * com a rede, via [RemoteGroupRepository]/[ExpenseRepository] (despesas do grupo entram no
 * payload de sync, mas não são exibidas nesta tela).
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
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val remoteGroupRepository: RemoteGroupRepository,
) : ViewModel() {

    /** Overrides transientes de [GroupSyncActionUiState] por grupo — só guarda InProgress/Failed. */
    private val syncStates = MutableStateFlow<Map<String, GroupSyncActionUiState>>(emptyMap())

    val uiState: StateFlow<GroupListUiState> = combine(
        groupRepository.getGroupsFlow(),
        authRepository.getSessionFlow(),
        syncStates,
    ) { groups, session, syncStatesSnapshot -> GroupsSnapshot(groups, session != null, syncStatesSnapshot) }
        .flatMapLatest { snapshot -> observeContent(snapshot) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GroupListUiState.Loading,
        )

    /**
     * T19.1/T19.2: envia grupo + participantes + despesas pro backend (`RemoteGroupRepository`);
     * ao sucesso, persiste `isSynced=true` + `remoteId` no Room (mesma transação de escrita que
     * já existia pra criar grupo, `GroupRepository.insertGroup` é upsert). Falha de rede nunca
     * chega a mudar o Room — `isSynced` continua `false`, só o estado transiente na tela vira
     * [GroupSyncActionUiState.Failed], sem deixar o grupo num estado inconsistente.
     */
    fun onSyncGroupClick(groupId: String) {
        if (syncStates.value[groupId] is GroupSyncActionUiState.InProgress) return

        viewModelScope.launch {
            syncStates.update { it + (groupId to GroupSyncActionUiState.InProgress) }
            try {
                syncGroup(groupId)
                syncStates.update { it - groupId }
            } catch (error: GroupSyncException) {
                val message = error.message ?: "Não foi possível sincronizar."
                syncStates.update { it + (groupId to GroupSyncActionUiState.Failed(message)) }
            }
        }
    }

    private suspend fun syncGroup(groupId: String) {
        val group = requireNotNull(groupRepository.getGroupById(groupId)) {
            "Grupo $groupId não encontrado pra sincronizar."
        }
        val participants = participantRepository.getParticipantsFlow(groupId).first()
        val expenses = expenseRepository.getExpensesFlow(groupId).first()

        val remoteId = remoteGroupRepository.syncGroup(group, participants, expenses)

        groupRepository.insertGroup(group.copy(isSynced = true, remoteId = remoteId))
    }

    private fun observeContent(snapshot: GroupsSnapshot): Flow<GroupListUiState> {
        if (snapshot.groups.isEmpty()) return flowOf(GroupListUiState.Empty)

        val itemFlows = snapshot.groups.map { group ->
            participantRepository.getParticipantsFlow(group.id)
                .map { participants ->
                    group.toUiModel(
                        participantCount = participants.size,
                        syncAction = group.syncActionFor(snapshot.isAuthenticated, snapshot.syncStates),
                    )
                }
        }
        return combine(itemFlows) { items -> GroupListUiState.Content(items.toList()) }
    }

    private data class GroupsSnapshot(
        val groups: List<Group>,
        val isAuthenticated: Boolean,
        val syncStates: Map<String, GroupSyncActionUiState>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Group.toUiModel(participantCount: Int, syncAction: GroupSyncActionUiState) = GroupListItemUiModel(
    id = id,
    name = name,
    tag = tagFor(name),
    participantCount = participantCount,
    isSynced = isSynced,
    balance = GroupBalance.Settled,
    syncAction = syncAction,
)

/**
 * Grupo já sincronizado ou usuário deslogado: ação escondida (não existe "sincronizar sem estar
 * logado", ver T19-app-sincronizar-grupo.md). Caso contrário, usa o override transiente
 * (InProgress/Failed da última tentativa) ou [GroupSyncActionUiState.Available] por padrão.
 */
private fun Group.syncActionFor(
    isAuthenticated: Boolean,
    overrides: Map<String, GroupSyncActionUiState>,
): GroupSyncActionUiState {
    if (isSynced || !isAuthenticated) return GroupSyncActionUiState.Hidden
    return overrides[id] ?: GroupSyncActionUiState.Available
}

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
