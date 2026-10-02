package com.even.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.even.app.R
import com.even.app.ui.groups.EvenBottomBar
import com.even.app.ui.groups.EvenBottomTab
import com.even.app.ui.theme.LocalEvenColors
import com.even.app.ui.theme.ThemeToggleButton
import com.even.domain.model.AuthenticatedUser

/**
 * Entry point for the login screen, faithful to `prototype/login.html`. `factory` injects
 * the [AuthViewModel] through [com.even.app.di.AppContainer]'s manual composition — the same
 * convention as [com.even.app.ui.groups.GroupListRoute]: the Composable itself doesn't know
 * where the state comes from or how the Google ID token is obtained.
 *
 * [onSignedIn] is optional: when non-null, it fires once as soon as [uiState] becomes
 * [AuthUiState.SignedIn], via [LaunchedEffect]. Used by the "join group via link" flow to resume
 * automatically after login — normal access to the screen (the profile icon) doesn't pass this
 * parameter and keeps no automatic navigation after signing in.
 */
@Composable
fun LoginRoute(
    factory: AuthViewModelFactory,
    onBackClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onSignedIn: (() -> Unit)? = null,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (onSignedIn != null && uiState is AuthUiState.SignedIn) onSignedIn()
    }

    LoginScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSignInClick = viewModel::signInWithGoogle,
        onSignOutClick = viewModel::signOut,
        onContinueWithoutAccount = onContinueWithoutAccount,
        unreadNotificationsCount = unreadNotificationsCount,
        onGroupsClick = onGroupsClick,
        onNotificationsClick = onNotificationsClick,
        authenticatedUserName = authenticatedUserName,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

/**
 * A thin Composable: picks between [SignInContent] and [AccountContent] based on [uiState] and
 * overlays [ConnectingOverlay] while connecting — a replica of `prototype/login.html`'s `#screen`
 * + `#overlay` (the overlay appears on top of the screen instead of replacing it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
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
            LoginTopBar(
                uiState = uiState,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        bottomBar = {
            EvenBottomBar(
                selectedTab = EvenBottomTab.PROFILE,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        EvenBottomTab.GROUPS -> onGroupsClick()
                        EvenBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        EvenBottomTab.PROFILE -> Unit
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is AuthUiState.SignedIn -> AccountContent(user = uiState.user, onSignOutClick = onSignOutClick)
                is AuthUiState.SignedOut -> SignInContent(
                    errorMessage = uiState.errorMessage,
                    onSignInClick = onSignInClick,
                    onContinueWithoutAccount = onContinueWithoutAccount,
                )
                AuthUiState.Connecting -> SignInContent(
                    errorMessage = null,
                    onSignInClick = onSignInClick,
                    onContinueWithoutAccount = onContinueWithoutAccount,
                )
            }
            if (uiState is AuthUiState.Connecting) ConnectingOverlay()
        }
    }
}

/** The prototype's `.topbar h1` — "Sign in" signed out/connecting, "Your account" signed in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTopBar(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalEvenColors.current
    val title = if (uiState is AuthUiState.SignedIn) "Your account" else "Sign in"
    TopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

/** Signed-out state: an explanatory `.card` + `.btn-google` + `.btn-ghost` "Continue without an account". */
@Composable
private fun SignInContent(
    errorMessage: String?,
    onSignInClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        SyncExplanationCard()
        Spacer(Modifier.height(16.dp))
        GoogleSignInButton(onClick = onSignInClick)
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = errorMessage, color = colors.danger, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(4.dp))
        ContinueWithoutAccountLink(onClick = onContinueWithoutAccount)
        Spacer(Modifier.height(18.dp))
    }
}

/** The prototype's `.card`: three reasons to sync, none required (principle 1). */
@Composable
private fun SyncExplanationCard(modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.paperAlt, shape = RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        ExplanationLine(text = "Using Even doesn't require an account. Signing in with Google is only to:")
        ExplanationLine(text = "· keep a group available on more than one device")
        ExplanationLine(text = "· notify you in real time when someone in the group logs an expense or settles a debt")
        ExplanationLine(text = "· keep the history of past groups", isLast = true)
    }
}

@Composable
private fun ExplanationLine(text: String, isLast: Boolean = false, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Text(
        text = text,
        color = colors.inkSoft,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        modifier = modifier.padding(bottom = if (isLast) 0.dp else 10.dp),
    )
}

/**
 * The prototype's `.btn-google`: its own surface tokens (`--google-bg` / `--google-ink`) so the
 * button still reads as Google's in both themes, with a `--control-border` boundary. The four
 * colours inside the **G** mark are Google's trademark and are deliberately *not* tokens:
 * `ic_google_logo` keeps them fixed (`tint = Color.Unspecified`) in both themes.
 */
@Composable
private fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.googleBg,
        border = BorderStroke(1.dp, colors.controlBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = "Sign in with Google", color = colors.googleInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** The prototype's `.btn-ghost` — always visible, never gated behind login (principle 1). */
@Composable
private fun ContinueWithoutAccountLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text = "Continue without an account", color = colors.inkSoft, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** The prototype's `#overlay`: `--overlay-scrim` + `.spinner` over the screen, without switching screens. */
@Composable
private fun ConnectingOverlay(modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.overlayScrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colors.brand,
                trackColor = colors.rule,
                strokeWidth = 3.dp,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(text = "Connecting to Google…", color = colors.inkSoft, fontSize = 13.sp)
        }
    }
}

/** Signed-in state: the "Your account" block (avatar + name + email), an explanation and a `.btn-secondary` "Sign out". */
@Composable
private fun AccountContent(user: AuthenticatedUser, onSignOutClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        AccountCard(user = user)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Synced groups become available on any device with this account, " +
                "and you get a real-time notification when someone logs an expense or settles a debt.",
            color = colors.inkSoft,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(16.dp))
        SignOutButton(onClick = onSignOutClick)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccountCard(user: AuthenticatedUser, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.paperAlt, shape = RoundedCornerShape(14.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(color = colors.brand, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initialsOf(user.name), color = colors.onBrand, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = user.name, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = user.email, color = colors.inkSoft, fontSize = 13.5.sp)
    }
}

/**
 * The prototype's `.btn-secondary`. Revokes the session on the backend and clears the session
 * stored on the device (via `AuthViewModel.signOut` → `RemoteAuthRepository.signOut`) — the
 * local cleanup always happens, even if the backend revocation fails due to no network.
 */
@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.paperAlt,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(text = "Sign out", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** Same heuristic as `initials()` in `prototype/app.js`: up to two initials, uppercase. */
private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { word -> word.isNotBlank() }
        .take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")

