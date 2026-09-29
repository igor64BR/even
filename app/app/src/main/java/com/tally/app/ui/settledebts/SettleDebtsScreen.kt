package com.tally.app.ui.settledebts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tally.app.ui.groups.TallyBottomBar
import com.tally.app.ui.groups.TallyBottomTab
import com.tally.app.ui.theme.LocalTallyColors

/**
 * "Settle debts" screen (T42.3, RF44), opened from the button of the same name in "Group details"
 * (T42.2). `factory` injects the [SettleDebtsViewModel] through the manual composition of
 * [com.tally.app.di.AppContainer], same pattern as the module's other screens.
 *
 * [key] = `"SettleDebts:$groupId"` (see `TallyApp` in `MainActivity`, same reasoning as
 * [com.tally.app.ui.groupdetail.GroupDetailRoute]) — without it, settling debts for a group
 * different from the last one visited would reuse the first one's `ViewModel`.
 */
@Composable
fun SettleDebtsRoute(
    factory: SettleDebtsViewModelFactory,
    onBackClick: () -> Unit,
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
    val viewModel: SettleDebtsViewModel = viewModel(factory = factory, key = key)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettleDebtsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onMarkAsPaidClick = viewModel::onMarkAsPaidClick,
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
 * Thin composable: delegates to the right body based on [uiState] — no balance/settlement
 * calculation lives here, [SettleDebtsViewModel] already delivers the ready-made list (Object
 * Calisthenics).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtsScreen(
    uiState: SettleDebtsUiState,
    onBackClick: () -> Unit,
    onMarkAsPaidClick: (SettlementSuggestionRowUiModel) -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            SettleDebtsTopBar(onBackClick = onBackClick, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        },
        bottomBar = {
            TallyBottomBar(
                selectedTab = TallyBottomTab.GROUPS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        TallyBottomTab.GROUPS -> onGroupsClick()
                        TallyBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        TallyBottomTab.PROFILE -> onProfileClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is SettleDebtsUiState.Loading -> Unit
                is SettleDebtsUiState.SettledUp -> SettledUpState(groupName = uiState.groupName)
                is SettleDebtsUiState.Content -> SettleDebtsContent(
                    suggestions = uiState.suggestions,
                    onMarkAsPaidClick = onMarkAsPaidClick,
                )
            }
        }
    }
}

@Composable
private fun SettleDebtsContent(
    suggestions: List<SettlementSuggestionRowUiModel>,
    onMarkAsPaidClick: (SettlementSuggestionRowUiModel) -> Unit,
) {
    val colors = LocalTallyColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "The smallest number of transfers to zero out all of the group's balances.",
            fontSize = 13.sp,
            color = colors.inkSoft,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        suggestions.forEach { suggestion ->
            SettlementSuggestionRow(suggestion = suggestion, onMarkAsPaidClick = onMarkAsPaidClick)
        }
    }
}
