package com.rateio.app.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.groups.RateioBottomBar
import com.rateio.app.ui.groups.RateioBottomTab
import com.rateio.app.ui.theme.LocalRateioColors
import com.rateio.app.ui.theme.ThemeToggleButton

/**
 * "Notifications" screen (T41.1, RF35/RF36), faithful to `prototype/notifications.html`.
 * `factory` injects the [NotificationsViewModel] via manual composition from
 * [com.rateio.app.di.AppContainer] — same pattern as the other screens.
 *
 * [DisposableEffect] calls [NotificationsViewModel.onScreenClosed] only when leaving the screen
 * (see that method's KDoc for why it doesn't mark as read right on entry).
 */
@Composable
fun NotificationsRoute(
    factory: NotificationsViewModelFactory,
    onGroupsClick: () -> Unit,
    onProfileClick: () -> Unit,
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: NotificationsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenClosed() }
    }

    NotificationsScreen(
        uiState = uiState,
        unreadNotificationsCount = unreadCount,
        onBackClick = onGroupsClick,
        onGroupsClick = onGroupsClick,
        onProfileClick = onProfileClick,
        authenticatedUserName = authenticatedUserName,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    unreadNotificationsCount: Int,
    onBackClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onProfileClick: () -> Unit,
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            NotificationsTopBar(
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        bottomBar = {
            RateioBottomBar(
                selectedTab = RateioBottomTab.NOTIFICATIONS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        RateioBottomTab.NOTIFICATIONS -> Unit
                        RateioBottomTab.GROUPS -> onGroupsClick()
                        RateioBottomTab.PROFILE -> onProfileClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NotificationsBody(uiState = uiState, onSignInClick = onProfileClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(onBackClick: () -> Unit, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val colors = LocalRateioColors.current
    TopAppBar(
        title = { Text(text = "Notifications", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { ThemeToggleButton(isDarkTheme = isDarkTheme, onToggleClick = onToggleTheme) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            actionIconContentColor = colors.ink,
        ),
    )
}

@Composable
private fun NotificationsBody(uiState: NotificationsUiState, onSignInClick: () -> Unit) {
    when (uiState) {
        is NotificationsUiState.Loading -> Unit
        is NotificationsUiState.RequiresAccount -> RequiresAccountState(onSignInClick = onSignInClick)
        is NotificationsUiState.Empty -> EmptyNotificationsState()
        is NotificationsUiState.Content -> NotificationsList(notifications = uiState.notifications)
    }
}

/** "Notifications require an account" — local groups have no one to notify, same copy as the prototype. */
@Composable
private fun RequiresAccountState(onSignInClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Notifications require an account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(
            text = "Local groups have no one to notify. Sign in with Google and sync a group " +
                "to start getting real-time alerts.",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Button(
            onClick = onSignInClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brandInk, contentColor = colors.onBrand),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(text = "Sign in with Google", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "All quiet here" — same copy as the prototype, for a synced group with no events yet. */
@Composable
private fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "All quiet here", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(
            text = "When someone adds an expense or settles a debt in a synced group, " +
                "it'll show up here instantly.",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun NotificationsList(notifications: List<NotificationRowUiModel>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(items = notifications, key = { it.id }) { notification -> NotificationRow(notification) }
    }
}

/** Prototype's `.notif-row`: dot (`--brand` if unread, transparent if read), text, relative time. */
@Composable
private fun NotificationRow(notification: NotificationRowUiModel) {
    val colors = LocalRateioColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(color = if (notification.isRead) colors.paper else colors.brand, shape = CircleShape),
        )
        Column {
            Text(text = notification.message, fontSize = 14.sp, lineHeight = 19.6.sp, color = colors.ink)
            Text(
                text = notification.relativeTime,
                fontSize = 11.5.sp,
                color = colors.inkSoft,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
