package com.even.app.ui.joingroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.even.app.ui.groups.EvenBottomBar
import com.even.app.ui.groups.EvenBottomTab
import com.even.app.ui.theme.LocalEvenColors
import com.even.app.ui.theme.ThemeToggleButton

/**
 * Entry point of the "join group via link" flow. `factory` injects the
 * [JoinGroupViewModel] through the manual composition of [com.even.app.di.AppContainer] — same
 * convention as [com.even.app.ui.auth.LoginRoute].
 *
 * [onNeedsLogin] is called (via [LaunchedEffect]) when [JoinGroupUiState.NeedsLogin] shows up —
 * whoever assembles navigation (`MainActivity`) decides what to do (send to the login screen,
 * keeping the code to resume afterwards). This Composable never navigates on its own.
 *
 * [key] = `"JoinGroup:$inviteCode"` (see `EvenApp` in `MainActivity`) — without it, opening a
 * second invite link would reuse the first one's `ViewModel`.
 */
@Composable
fun JoinGroupRoute(
    factory: JoinGroupViewModelFactory,
    onNeedsLogin: (inviteCode: String) -> Unit,
    onDone: () -> Unit,
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
    val viewModel: JoinGroupViewModel = viewModel(factory = factory, key = key)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val needsLogin = uiState as? JoinGroupUiState.NeedsLogin ?: return@LaunchedEffect
        onNeedsLogin(needsLogin.inviteCode)
    }

    JoinGroupScreen(
        uiState = uiState,
        onConfirmClick = viewModel::confirm,
        onRetryClick = viewModel::retry,
        onCancelClick = onDone,
        onDoneClick = onDone,
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
 * Thin composable: picks the content based on [uiState]. [JoinGroupUiState.NeedsLogin] and
 * [JoinGroupUiState.CheckingSession] just render a loading indicator — [JoinGroupRoute] already
 * navigates away as soon as [JoinGroupUiState.NeedsLogin] shows up, so these two states only ever
 * appear for an instant (same behavior tolerated in [com.even.app.ui.auth.AuthViewModel] before
 * the first session emission).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    uiState: JoinGroupUiState,
    onConfirmClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDoneClick: () -> Unit,
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

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = { Text(text = "Join group", fontWeight = FontWeight.Bold) },
                actions = { ThemeToggleButton(isDarkTheme = isDarkTheme, onToggleClick = onToggleTheme) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.paper,
                    titleContentColor = colors.ink,
                    actionIconContentColor = colors.ink,
                ),
            )
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            when (uiState) {
                JoinGroupUiState.CheckingSession, is JoinGroupUiState.NeedsLogin -> LoadingContent()
                is JoinGroupUiState.Confirming -> ConfirmContent(onConfirmClick = onConfirmClick, onCancelClick = onCancelClick)
                JoinGroupUiState.Joining -> LoadingContent()
                is JoinGroupUiState.Success -> ResultContent(
                    message = "You joined the group. Sync the app again to see the full details " +
                        "once that part of the backend exists.",
                    onDoneClick = onDoneClick,
                )
                is JoinGroupUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetryClick = onRetryClick,
                    onCancelClick = onCancelClick,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    CircularProgressIndicator(
        color = colors.brand,
        trackColor = colors.rule,
        strokeWidth = 3.dp,
        modifier = modifier.size(28.dp),
    )
}

/**
 * "You've been invited to join a group" — deliberately generic text: there's no endpoint to
 * preview the group by its code, so there's no name to show here ("don't make one up").
 */
@Composable
private fun ConfirmContent(onConfirmClick: () -> Unit, onCancelClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colors.paperAlt,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You've been invited to join a group",
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
        Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onConfirmClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Join group")
            }
            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Cancel", color = colors.inkSoft)
            }
        }
    }
}

@Composable
private fun ResultContent(message: String, onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = colors.ink, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(onClick = onDoneClick, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(text = "OK")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = colors.danger,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Column(
            Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onRetryClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Try again")
            }
            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Cancel", color = colors.inkSoft)
            }
        }
    }
}
