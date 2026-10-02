package com.even.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.even.domain.model.Group
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.NotificationRepository
import com.even.domain.repository.ParticipantRepository
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
 * State for the "Your groups" screen. The data source is [GroupRepository] +
 * [ParticipantRepository] — the two `:domain` ports that `:data` already implements over Room;
 * zero network calls to build the list.
 *
 * [Group.isSynced] is a real field, and [toUiModel] uses its actual value.
 * [GroupListItemUiModel.balance] still defaults to [GroupBalance.Settled]: computing a real
 * balance depends on the debt-simplification engine in Kotlin, which already exists and is
 * already used by [com.even.app.ui.groupdetail.GroupDetailViewModel] — bringing that calculation
 * over here, to the list, is a future extension, not implemented for this screen yet.
 *
 * The "Sync this group" action doesn't live here: it's a dependency on
 * [com.even.domain.repository.AuthRepository]/[com.even.domain.repository.RemoteGroupRepository]
 * that lives in [com.even.app.ui.groupdetail.GroupDetailViewModel], alongside everything else a
 * specific group can do.
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

    /** Badge for the "Notifications" tab — same source `NotificationsViewModel` uses. */
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
