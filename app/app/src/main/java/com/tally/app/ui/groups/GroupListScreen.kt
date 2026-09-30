package com.tally.app.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tally.app.ui.theme.LocalTallyColors

/**
 * "Your groups" screen, the app's entry point. `factory` injects the
 * [GroupListViewModel] through the manual composition of [com.tally.app.di.AppContainer] (no DI
 * framework yet) — the Composable itself doesn't know where the state comes from.
 */
@Composable
fun GroupListRoute(
    factory: GroupListViewModelFactory,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: GroupListViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    GroupListScreen(
        uiState = uiState,
        unreadNotificationsCount = unreadNotificationsCount,
        onCreateGroupClick = onCreateGroupClick,
        onGroupClick = onGroupClick,
        onProfileClick = onProfileClick,
        onNotificationsClick = onNotificationsClick,
        authenticatedUserName = authenticatedUserName,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

/**
 * Thin composable: only orchestrates [GroupListTopBar], the body (list/empty/loading),
 * [CreateGroupFab] and [TallyBottomBar] — none of them know where the state comes from (Object
 * Calisthenics: one responsibility per Composable, named after what it presents).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    uiState: GroupListUiState,
    unreadNotificationsCount: Int = 0,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { GroupListTopBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme) },
        floatingActionButton = { CreateGroupFab(onClick = onCreateGroupClick) },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            TallyBottomBar(
                selectedTab = TallyBottomTab.GROUPS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    // "Profile" is the equivalent of the prototype's auth-slot (see
                    // prototype/app.js, renderHeaderAuth); "Notifications" opens the notification
                    // center. "Groups" is the current screen, it doesn't navigate.
                    when (tab) {
                        TallyBottomTab.GROUPS -> Unit
                        TallyBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        TallyBottomTab.PROFILE -> onProfileClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GroupListBody(
                uiState = uiState,
                onCreateGroupClick = onCreateGroupClick,
                onGroupClick = onGroupClick,
            )
        }
    }
}

/**
 * The loading state has no dedicated visual treatment in the prototype (it's just synchronous
 * localStorage there); here, while the first Room value hasn't arrived yet, the screen stays
 * blank for an instant — acceptable for this task, a dedicated spinner is left for when there's a
 * real need for one (e.g. sync, which is actually asynchronous).
 */
@Composable
private fun GroupListBody(
    uiState: GroupListUiState,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
) {
    when (uiState) {
        is GroupListUiState.Loading -> Unit
        is GroupListUiState.Empty -> EmptyGroupsState(onCreateGroupClick = onCreateGroupClick)
        is GroupListUiState.Content -> GroupList(groups = uiState.groups, onGroupClick = onGroupClick)
    }
}

@Composable
private fun GroupList(groups: List<GroupListItemUiModel>, onGroupClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(items = groups, key = { it.id }) { group ->
            GroupCard(group = group, onClick = onGroupClick)
        }
    }
}
