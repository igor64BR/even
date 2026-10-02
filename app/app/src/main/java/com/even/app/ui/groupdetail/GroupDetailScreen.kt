package com.even.app.ui.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.even.app.ui.groups.EvenBottomBar
import com.even.app.ui.groups.EvenBottomTab
import com.even.app.ui.groups.SyncStatusIcon
import com.even.app.ui.theme.LocalEvenColors

/**
 * The "Group details" screen, opened from [com.even.app.ui.groups.GroupCard]. `factory` injects
 * the [GroupDetailViewModel] through [com.even.app.di.AppContainer]'s manual composition, the
 * same pattern as [com.even.app.ui.createexpense.CreateExpenseRoute].
 *
 * `DisposableEffect` ties the SignalR client to this screen's presence in the composition —
 * connects on entry, disconnects on exit, independent of the `ViewModel`'s own lifecycle. It's
 * this `DisposableEffect`, not the `ViewModel`, that guarantees the connection is only open while
 * the screen is being viewed.
 *
 * [key] = `"GroupDetail:$groupId"` (see `EvenApp` in `MainActivity`) — with no real
 * `NavHost`/back stack, every destination shares the same `ViewModelStoreOwner`; without this
 * `key`, opening a group different from the last one visited would reuse the first one's
 * `ViewModel` (and the `groupId` locked into it), showing the wrong group.
 */
@Composable
fun GroupDetailRoute(
    factory: GroupDetailViewModelFactory,
    onBackClick: () -> Unit,
    onCreateExpenseClick: () -> Unit,
    onEditExpenseClick: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    key: String? = null,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: GroupDetailViewModel = viewModel(factory = factory, key = key)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        viewModel.startRealtimeUpdates()
        onDispose { viewModel.stopRealtimeUpdates() }
    }

    GroupDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onCreateExpenseClick = onCreateExpenseClick,
        onEditExpenseClick = onEditExpenseClick,
        onDeleteExpenseConfirmed = viewModel::onDeleteExpenseClick,
        onSettleDebtsClick = onSettleDebtsClick,
        onSyncClick = viewModel::onSyncGroupClick,
        unreadNotificationsCount = unreadNotificationsCount,
        onGroupsClick = onGroupsClick,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        authenticatedUserName = authenticatedUserName,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

/**
 * A thin Composable: title/state comes from [uiState], the body delegates to
 * [GroupDetailContent]/loading states — none of them compute balance/split (Object Calisthenics,
 * same pattern as [com.even.app.ui.groups.GroupListScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    uiState: GroupDetailUiState,
    onBackClick: () -> Unit,
    onCreateExpenseClick: () -> Unit,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    onSyncClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current
    val title = (uiState as? GroupDetailUiState.Content)?.groupName ?: "Group"

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            GroupDetailTopBar(
                groupName = title,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        floatingActionButton = {
            if (uiState is GroupDetailUiState.Content) {
                FloatingActionButton(
                    onClick = onCreateExpenseClick,
                    containerColor = colors.brand,
                    contentColor = colors.onBrand,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "New expense")
                }
            }
        },
        bottomBar = {
            EvenBottomBar(
                selectedTab = EvenBottomTab.GROUPS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        EvenBottomTab.GROUPS -> onGroupsClick()
                        EvenBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        EvenBottomTab.PROFILE -> onProfileClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is GroupDetailUiState.Loading -> Unit
                is GroupDetailUiState.NotFound -> GroupNotFoundState()
                is GroupDetailUiState.Content -> GroupDetailContent(
                    uiState = uiState,
                    onEditExpenseClick = onEditExpenseClick,
                    onDeleteExpenseConfirmed = onDeleteExpenseConfirmed,
                    onSettleDebtsClick = onSettleDebtsClick,
                    onSyncClick = onSyncClick,
                )
            }
        }
    }
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailUiState.Content,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    val colors = LocalEvenColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        GroupEyebrow(participantCount = uiState.participantCount, isSynced = uiState.isSynced)

        SectionLabel(text = "Balances")
        Column {
            uiState.balances.forEach { participant -> ParticipantBalanceRow(participant = participant) }
        }

        // `.btn-accent` in `group.html`: amber, not indigo — this is the screen's one "money
        // changes hands" action, and the prototype reserves the accent for exactly that.
        Button(
            onClick = onSettleDebtsClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(text = "Settle debts", fontWeight = FontWeight.SemiBold)
        }

        SyncSection(syncAction = uiState.syncAction, onSyncClick = onSyncClick)

        SectionLabel(text = "Expenses")
        if (uiState.expenses.isEmpty()) {
            EmptyExpensesState()
        } else {
            ExpenseList(
                expenses = uiState.expenses,
                onEditExpenseClick = onEditExpenseClick,
                onDeleteExpenseConfirmed = onDeleteExpenseConfirmed,
            )
        }

        if (uiState.settlements.isNotEmpty()) {
            SectionLabel(text = "Settlement history")
            Column {
                uiState.settlements.forEach { settlement -> SettlementRow(settlement = settlement) }
            }
        }
    }
}

/**
 * [ExpenseDeleteConfirmationState] holds which expense has a pending deletion — the first
 * tap on [ExpenseRow]'s trash icon only reaches
 * [ExpenseDeleteConfirmationState.request] (opens the dialog), never deletes directly; only
 * "Delete" in the [AlertDialog] calls [ExpenseDeleteConfirmationState.confirm], which is what
 * triggers [onDeleteExpenseConfirmed]. Pure UI state (doesn't survive rotation/process death, no
 * need to — reopening the confirmation is cheap), hence `remember` instead of living in the
 * `ViewModel`.
 */
@Composable
private fun ExpenseList(
    expenses: List<ExpenseRowUiModel>,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteConfirmation = remember { ExpenseDeleteConfirmationState() }

    Column(modifier = modifier) {
        expenses.forEach { expense ->
            ExpenseRow(
                expense = expense,
                onClick = { onEditExpenseClick(expense.id) },
                onDeleteClick = { deleteConfirmation.request(expense.id) },
            )
        }
    }

    if (deleteConfirmation.pendingExpenseId != null) {
        DeleteExpenseConfirmationDialog(
            onConfirm = { deleteConfirmation.confirm(onConfirmed = onDeleteExpenseConfirmed) },
            onDismiss = deleteConfirmation::dismiss,
        )
    }
}

@Composable
private fun DeleteExpenseConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEvenColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete expense?", fontWeight = FontWeight.Bold) },
        text = { Text(text = "This expense disappears from the list and the group's balance is recalculated. This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = colors.danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun GroupEyebrow(participantCount: Int, isSynced: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(top = 4.dp),
    ) {
        Text(
            text = "$participantCount ${if (participantCount == 1) "person" else "people"}",
            fontSize = 11.sp,
            color = colors.inkSoft,
        )
        SyncStatusIcon(isSynced = isSynced)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        color = colors.inkSoft,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

/**
 * The "Sync this group" action. Only draws something when an action is available — an
 * already-synced group or a signed-out user get no extra room on the screen (the same rule as
 * [com.even.app.ui.groupdetail.GroupSyncActionUiState.Hidden]).
 */
@Composable
private fun SyncSection(syncAction: GroupSyncActionUiState, onSyncClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    when (syncAction) {
        GroupSyncActionUiState.Hidden -> Unit

        GroupSyncActionUiState.Available -> Button(
            onClick = onSyncClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.paperAlt, contentColor = colors.ink),
            modifier = modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            Text(text = "Sync this group", fontWeight = FontWeight.SemiBold)
        }

        GroupSyncActionUiState.InProgress -> Text(
            text = "Syncing…",
            color = colors.inkSoft,
            fontSize = 13.sp,
            modifier = modifier.padding(top = 10.dp),
        )

        is GroupSyncActionUiState.Failed -> Text(
            text = "${syncAction.message} — tap to try again",
            color = colors.danger,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = modifier.padding(top = 10.dp).clickable(onClick = onSyncClick),
        )
    }
}

@Composable
private fun GroupNotFoundState(modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(modifier = modifier.fillMaxWidth().padding(top = 60.dp, start = 20.dp, end = 20.dp)) {
        Text(text = "Group not found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.ink)
    }
}
