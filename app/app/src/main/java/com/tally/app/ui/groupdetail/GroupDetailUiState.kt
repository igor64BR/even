package com.tally.app.ui.groupdetail

/**
 * State of the "Group details" screen (T42.2, RF42). [Content] is the only state with real data —
 * [Loading] covers the instant before the first combined value arrives (Room + engine, T33) and
 * [NotFound] covers a `groupId` that no longer exists in Room (group deleted from another
 * tab/screen while this one was open; shouldn't happen in the normal flow, but avoids a crash
 * instead of assuming the group always exists).
 */
sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data object NotFound : GroupDetailUiState

    data class Content(
        val groupName: String,
        val participantCount: Int,
        val isSynced: Boolean,
        val balances: List<ParticipantBalanceUiModel>,
        val expenses: List<ExpenseRowUiModel>,
        val settlements: List<SettlementRowUiModel>,
        val syncAction: GroupSyncActionUiState,
    ) : GroupDetailUiState
}

/**
 * A row in "Balances" (the prototype `group.html`'s `.split-row`): a participant's balance,
 * computed by [com.tally.domain.engine.DebtSimplificationEngine.computeBalances] (T33) — never
 * recalculated here, `ParticipantBalanceRow` just presents the already-ready [balance].
 */
data class ParticipantBalanceUiModel(
    val participantId: String,
    val name: String,
    val isYou: Boolean,
    val balance: ParticipantBalance,
)

/**
 * The balance of ONE group participant, generic semantics ("gets back"/"owes", `group.html` ->
 * `saldosHtml`) — don't confuse this with [com.tally.app.ui.groups.GroupBalance], which is always
 * from "your" perspective (used only in the list card). Here any participant can be in any of the
 * three states, including the device owner.
 */
sealed interface ParticipantBalance {
    data object Settled : ParticipantBalance
    data class Owed(val amountCents: Long) : ParticipantBalance
    data class Credit(val amountCents: Long) : ParticipantBalance
}

/**
 * A row in "Expenses" (the prototype's `.expense-row`): description, who paid, a short date, split
 * type and total amount — no split calculation lives here, [splitTypeLabel] just translates the
 * `ExpenseSplit` (T7B) subtype already chosen when the expense was logged (T24).
 */
data class ExpenseRowUiModel(
    val id: String,
    val description: String,
    val payerName: String,
    val dateLabel: String,
    val amountCents: Long,
    val splitTypeLabel: String,
)

/**
 * A row in "Settlement history" (T37, RF31/RF33): who paid, who received, how much and when —
 * presentation only, `SettleDebtsViewModel` already recorded the settlement itself (T42.1); this
 * list never recalculates anything, it's purely the history of what already happened.
 */
data class SettlementRowUiModel(
    val id: String,
    val payerName: String,
    val receiverName: String,
    val dateLabel: String,
    val amountCents: Long,
)

/**
 * State of the "Sync this group" action (T19) for the detail screen. Until T19/before T42.4 this
 * action lived in the group list card (`com.tally.app.ui.groups.GroupSyncActionUiState`), a
 * temporary shortcut because this screen didn't exist yet. T42.4 moves the action here — it's the
 * right place now that "Group details" exists.
 */
sealed interface GroupSyncActionUiState {
    /** The group is already synced, or the user isn't authenticated — offering the action makes no sense. */
    data object Hidden : GroupSyncActionUiState

    /** A local group, an authenticated user: can tap to sync. */
    data object Available : GroupSyncActionUiState

    /** A call in progress. */
    data object InProgress : GroupSyncActionUiState

    /**
     * A network/HTTP failure on the last attempt — `Group.isSynced` stays `false` (no inconsistent
     * state), [message] already comes ready for the screen (see
     * [com.tally.domain.repository.GroupSyncException]).
     */
    data class Failed(val message: String) : GroupSyncActionUiState
}
