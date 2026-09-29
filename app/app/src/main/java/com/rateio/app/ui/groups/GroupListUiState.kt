package com.rateio.app.ui.groups

/** State for the "Your groups" screen (RF40/RF41) — loading, no groups, or a loaded list. */
sealed interface GroupListUiState {
    data object Loading : GroupListUiState
    data object Empty : GroupListUiState
    data class Content(val groups: List<GroupListItemUiModel>) : GroupListUiState
}

/**
 * A card in the list. Replica of what `index.html` builds per group: group initials
 * (`group-tag`), name, participant count + sync icon, and a colored balance.
 *
 * Up until T19 the "Sync this group" action lived in this model/card, as a temporary shortcut
 * because a group detail screen (RF42) didn't exist yet. T42.4 closes that gap: the sync action
 * now lives in [com.rateio.app.ui.groupdetail.GroupDetailUiState] — the card goes back to being
 * pure presentation, with no transient network state.
 */
data class GroupListItemUiModel(
    val id: String,
    val name: String,
    val tag: String,
    val participantCount: Int,
    val isSynced: Boolean,
    val balance: GroupBalance,
)

/**
 * The user's balance in the group, already in the prototype's display semantics
 * (`balanceInfo()` in `index.html`): "you are owed" (green/credit), "you owe" (terracotta/owed) or
 * "settled" (neutral).
 */
sealed interface GroupBalance {
    data object Settled : GroupBalance
    data class YouAreOwed(val amountCents: Long) : GroupBalance
    data class YouOwe(val amountCents: Long) : GroupBalance
}
