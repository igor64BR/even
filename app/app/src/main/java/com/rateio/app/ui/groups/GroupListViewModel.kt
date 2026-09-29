package com.rateio.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.model.Group
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.NotificationRepository
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
 * State for the "Your groups" screen (RF40/RF41). The data source is [GroupRepository] +
 * [ParticipantRepository] — the two `:domain` ports that `:data` already implements over Room
 * (T7); zero network calls to build the list, as required by T8.
 *
 * Gap known since T8 (which reported it instead of making up data, since it was scoped to
 * `app/app/`): `Group` was missing a sync flag and `Expense` a per-participant split. T7B
 * (`:domain`/`:data`) fixed the first half — [Group.isSynced] is now a real field, and
 * [toUiModel] uses the real value instead of the fixed `false` T8 had put in as a placeholder.
 * [GroupListItemUiModel.balance] still defaults to [GroupBalance.Settled]: computing a real
 * balance depends on the debt-simplification engine in Kotlin (RF25-RF28, T33), which already
 * exists and is already used by [com.rateio.app.ui.groupdetail.GroupDetailViewModel] — bringing
 * that calculation over here, to the list, is a future extension outside the scope of T42 (which
 * only calls for the detail/settlement screen), not a fix for this task.
 *
 * T19 had put the "Sync this group" action here, as a temporary shortcut because a group detail
 * screen (RF42) didn't exist yet. T42.4 removes this dependency on
 * [com.rateio.domain.repository.AuthRepository]/[com.rateio.domain.repository.RemoteGroupRepository]
 * — the action now lives in [com.rateio.app.ui.groupdetail.GroupDetailViewModel], alongside
 * everything else a specific group can do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModel(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> = groupRepository.getGroupsFlow()
        .flatMapLatest { groups -> observeContent(groups) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GroupListUiState.Loading,
        )

    /** Badge for the "Notifications" tab (T41.2) — same source `NotificationsViewModel` uses. */
    val unreadNotificationsCount: StateFlow<Int> = notificationRepository.getUnreadCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
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
 * Two-letter initials for the card's `group-tag` — same heuristic as `index.html`: first
 * "significant" word (more than 2 letters or starting with an uppercase letter), falling back to
 * the full name.
 */
private fun tagFor(name: String): String {
    val significantWord = name.split(" ")
        .firstOrNull { word -> word.length > 2 || word.firstOrNull()?.isUpperCase() == true }
    return (significantWord ?: name).take(2).uppercase()
}
