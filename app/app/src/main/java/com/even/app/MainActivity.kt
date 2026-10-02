package com.even.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.even.app.di.AppContainer
import com.even.domain.model.ThemePreference
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.even.app.ui.auth.AuthViewModelFactory
import com.even.app.ui.auth.LoginRoute
import com.even.app.ui.creategroup.CreateGroupRoute
import com.even.app.ui.creategroup.CreateGroupViewModelFactory
import com.even.app.ui.createexpense.CreateExpenseRoute
import com.even.app.ui.createexpense.CreateExpenseViewModelFactory
import com.even.app.ui.groupdetail.GroupDetailRoute
import com.even.app.ui.groupdetail.GroupDetailViewModelFactory
import com.even.app.ui.groups.GroupListRoute
import com.even.app.ui.groups.GroupListViewModelFactory
import com.even.app.ui.joingroup.JoinGroupRoute
import com.even.app.ui.joingroup.JoinGroupViewModelFactory
import com.even.app.ui.notifications.NotificationsRoute
import com.even.app.ui.notifications.NotificationsViewModelFactory
import com.even.app.ui.settledebts.SettleDebtsRoute
import com.even.app.ui.settledebts.SettleDebtsViewModelFactory
import com.even.app.ui.theme.EvenTheme
import com.even.app.ui.theme.resolveIsDarkTheme
import java.util.UUID

/**
 * The app's entry point — opens straight into "Your groups", no login required. Only sets up the
 * theme and delegates to [GroupListRoute]; no UI logic lives here.
 *
 * [pendingInviteCode] is the only platform logic that needs to live in the Activity instead of a
 * ViewModel: extracting the invite code from an [Intent] (deep link `even://join/{code}`,
 * `AndroidManifest.xml`) requires `Intent`/`Uri`, which don't make sense leaking into
 * `:domain`/ViewModels. `android:launchMode="singleTop"` guarantees that reopening the link while
 * the app is already in memory reaches [onNewIntent] instead of recreating the Activity.
 */
class MainActivity : ComponentActivity() {

    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingInviteCode = intent.extractInviteCode()

        val container = (application as EvenApplication).container
        setContent {
            EvenApp(
                container = container,
                pendingInviteCode = pendingInviteCode,
                onPendingInviteCodeConsumed = { pendingInviteCode = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingInviteCode = intent.extractInviteCode()
    }
}

/**
 * `even://join/{code}` — the fixed scheme and host of the deep link, the invite code in the
 * first path segment. `null` for any intent that isn't this link (a normal launcher open, other
 * actions). `internal` (instead of `private`) just to be directly testable — the same convention
 * as [com.even.app.ui.creategroup.FieldLabel].
 */
internal fun Intent?.extractInviteCode(): String? {
    val uri = this?.data ?: return null
    if (action != Intent.ACTION_VIEW) return null
    if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) return null
    return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
}

private const val DEEP_LINK_SCHEME = "even"
private const val DEEP_LINK_HOST = "join"

/**
 * Navigable destinations from the root. No NavHost yet (a real bottom nav doesn't exist) — this
 * is a minimal state machine between the screens that already exist.
 *
 * [GroupDetail] is the real destination for tapping a [com.even.app.ui.groups.GroupCard].
 * [CreateExpense] and [SettleDebts] are reached from [GroupDetail], not directly from the list.
 *
 * [Login.pendingInviteCode] carries the intent to join a group when the deep link arrives with the
 * user signed out — `null` on normal access (the profile icon). [JoinGroup] is the confirmation
 * screen's destination, reached directly from the deep link (user signed in) or resumed after
 * [Login] (user signed in first).
 *
 * [Notifications] is the destination of the bottom nav's "Notifications" tab — `EvenBottomBar` is
 * present on every screen reachable from the root (the same pattern as the prototype:
 * `group.html`/`new-expense.html`/`settle.html`/`create-group.html`/`login.html` always have
 * `.bottombar`, with "Groups" marked active on screens that are a sub-flow of the group list
 * and "Profile" active in [Login]), not just in [GroupList]/[Notifications].
 *
 * [CreateExpense.expenseId] is `null` for "New expense" (reached from [GroupDetail]'s FAB) and the
 * id of the expense being edited when it comes from [GroupDetail]'s `onEditExpenseClick` (tapping
 * an [com.even.app.ui.groupdetail.ExpenseRow]) — the same destination for both flows, only the
 * parameter changes (editing is state, not a new screen).
 *
 * [CreateGroup.instanceId]/[CreateExpense.instanceId]: with no `NavHost`, every destination shares
 * the same `ViewModelStoreOwner` (the Activity itself) — `viewModel(factory=...)` with no explicit
 * `key` caches by class, not by navigation, so reopening "New group"/"New expense" returned the
 * previous visit's `ViewModel`, with the whole form (and `isSaving`) still in its last submission's
 * state. `instanceId` generates a new value on every `EvenDestination.CreateGroup()`/
 * `CreateExpense()` built (defaulting to `UUID.randomUUID()`), becomes that `viewModel()`'s `key`
 * (see `EvenApp`) and guarantees a fresh form on every visit. [GroupDetail]/[SettleDebts] don't
 * need this — their `ViewModel` is just a mirror of Room `Flow`s (no "isSaving" or form field to go
 * stale), so their `key` is just the `groupId`: revisiting the SAME group reuses the instance
 * (cheap, harmless), visiting a DIFFERENT group already forces a new one (which was the real bug
 * there — without this, the second group opened would show the first one's data).
 */
private sealed interface EvenDestination {
    data object GroupList : EvenDestination
    data class CreateGroup(val instanceId: String = UUID.randomUUID().toString()) : EvenDestination
    data class GroupDetail(val groupId: String) : EvenDestination
    data class CreateExpense(
        val groupId: String,
        val expenseId: String? = null,
        val instanceId: String = UUID.randomUUID().toString(),
    ) : EvenDestination
    data class SettleDebts(val groupId: String) : EvenDestination
    data class Login(val pendingInviteCode: String? = null) : EvenDestination
    data class JoinGroup(val inviteCode: String) : EvenDestination
    data object Notifications : EvenDestination
}

@Composable
private fun EvenApp(
    container: AppContainer,
    pendingInviteCode: String? = null,
    onPendingInviteCodeConsumed: () -> Unit = {},
) {
    var destination by remember { mutableStateOf<EvenDestination>(EvenDestination.GroupList) }

    // Unread badge hoisted here instead of injected into each ViewModel: it's the only
    // piece of state every screen behind EvenBottomBar needs, and none of them (expense editing,
    // create group, login, ...) has any other reason to know about NotificationRepository — adding
    // that dependency to each one just to paint a badge would violate each ViewModel's single
    // responsibility. GroupList/Notifications keep their own source (it already existed before this
    // screen got `EvenBottomBar` everywhere).
    val unreadNotificationsCount by container.notificationRepository.getUnreadCountFlow()
        .collectAsStateWithLifecycle(initialValue = 0)

    // Authenticated user's name (or `null` if signed out) hoisted for the same reason as the badge
    // above — swaps the "Sign in"/"Profile" label and icon (generic/initials avatar) of
    // `EvenBottomBar` on every screen, mirroring the prototype's `renderHeaderAuth`
    // (`prototype/app.js`).
    val authenticatedUserName by container.authRepository.getSessionFlow()
        .map { session -> session?.user?.name }
        .collectAsStateWithLifecycle(initialValue = null)

    // Sun/moon button (the same global component as `prototype/app.js`'s `initThemeToggle`,
    // present in the TopAppBar on every screen). The stored preference is `SYSTEM` until the user
    // taps the button for the first time — in that case the app follows the device's light/dark
    // setting, just like the prototype followed `prefers-color-scheme` before any choice was saved
    // to `localStorage`. After the first tap, the saved LIGHT/DARK rules, on every future launch,
    // until the user taps again.
    val themePreference by container.themeRepository.getThemePreferenceFlow()
        .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
    val isDarkTheme = themePreference.resolveIsDarkTheme(isSystemInDarkTheme())
    val coroutineScope = rememberCoroutineScope()
    val onToggleTheme: () -> Unit = {
        val toggled = if (isDarkTheme) ThemePreference.LIGHT else ThemePreference.DARK
        coroutineScope.launch { container.themeRepository.setThemePreference(toggled) }
    }

    // Deep link `even://join/{code}` (extracted from the Intent in MainActivity). Routes
    // straight to JoinGroup regardless of session: it's JoinGroupViewModel itself that checks login
    // and exposes NeedsLogin — EvenApp only reacts to that state (below, in the JoinGroup case) by
    // sending to Login with the stored code, without duplicating the session check here.
    LaunchedEffect(pendingInviteCode) {
        val code = pendingInviteCode ?: return@LaunchedEffect
        destination = EvenDestination.JoinGroup(code)
        onPendingInviteCodeConsumed()
    }

    val groupListViewModelFactory = GroupListViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
        notificationRepository = container.notificationRepository,
    )
    val notificationsViewModelFactory = NotificationsViewModelFactory(
        notificationRepository = container.notificationRepository,
        authRepository = container.authRepository,
    )
    val createGroupViewModelFactory = CreateGroupViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )
    val authViewModelFactory = AuthViewModelFactory(
        authRepository = container.authRepository,
        googleIdentityClient = container.googleIdentityClient,
    )

    EvenTheme(darkTheme = isDarkTheme) {
        when (val current = destination) {
            EvenDestination.GroupList -> GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { destination = EvenDestination.CreateGroup() },
                onGroupClick = { groupId -> destination = EvenDestination.GroupDetail(groupId) },
                onProfileClick = { destination = EvenDestination.Login() },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )

            is EvenDestination.CreateGroup -> CreateGroupRoute(
                factory = createGroupViewModelFactory,
                key = "CreateGroup:${current.instanceId}",
                onGroupCreated = { destination = EvenDestination.GroupList },
                onBackClick = { destination = EvenDestination.GroupList },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                onProfileClick = { destination = EvenDestination.Login() },
            )

            is EvenDestination.GroupDetail -> GroupDetailRoute(
                factory = GroupDetailViewModelFactory(
                    groupId = current.groupId,
                    groupRepository = container.groupRepository,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    settlementRepository = container.settlementRepository,
                    authRepository = container.authRepository,
                    remoteGroupRepository = container.remoteGroupRepository,
                    remoteExpenseRepository = container.remoteExpenseRepository,
                    debtSimplificationEngine = container.debtSimplificationEngine,
                    groupRealtimeGateway = container.groupRealtimeGateway,
                ),
                key = "GroupDetail:${current.groupId}",
                onBackClick = { destination = EvenDestination.GroupList },
                onCreateExpenseClick = { destination = EvenDestination.CreateExpense(current.groupId) },
                onEditExpenseClick = { expenseId ->
                    destination = EvenDestination.CreateExpense(current.groupId, expenseId)
                },
                onSettleDebtsClick = { destination = EvenDestination.SettleDebts(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                onProfileClick = { destination = EvenDestination.Login() },
            )

            is EvenDestination.CreateExpense -> CreateExpenseRoute(
                factory = CreateExpenseViewModelFactory(
                    groupId = current.groupId,
                    expenseId = current.expenseId,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    groupRepository = container.groupRepository,
                    remoteExpenseRepository = container.remoteExpenseRepository,
                ),
                key = "CreateExpense:${current.instanceId}",
                onSaved = { destination = EvenDestination.GroupDetail(current.groupId) },
                onBackClick = { destination = EvenDestination.GroupDetail(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                onProfileClick = { destination = EvenDestination.Login() },
            )

            is EvenDestination.SettleDebts -> SettleDebtsRoute(
                factory = SettleDebtsViewModelFactory(
                    groupId = current.groupId,
                    groupRepository = container.groupRepository,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    settlementRepository = container.settlementRepository,
                    debtSimplificationEngine = container.debtSimplificationEngine,
                ),
                key = "SettleDebts:${current.groupId}",
                onBackClick = { destination = EvenDestination.GroupDetail(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                onProfileClick = { destination = EvenDestination.Login() },
            )

            is EvenDestination.Login -> LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { destination = EvenDestination.GroupList },
                onContinueWithoutAccount = { destination = EvenDestination.GroupList },
                onSignedIn = current.pendingInviteCode?.let { code ->
                    { destination = EvenDestination.JoinGroup(code) }
                },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
            )

            is EvenDestination.JoinGroup -> JoinGroupRoute(
                factory = JoinGroupViewModelFactory(
                    inviteCode = current.inviteCode,
                    authRepository = container.authRepository,
                    remoteGroupRepository = container.remoteGroupRepository,
                ),
                key = "JoinGroup:${current.inviteCode}",
                onNeedsLogin = { code -> destination = EvenDestination.Login(pendingInviteCode = code) },
                onDone = { destination = EvenDestination.GroupList },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onNotificationsClick = { destination = EvenDestination.Notifications },
                onProfileClick = { destination = EvenDestination.Login() },
            )

            EvenDestination.Notifications -> NotificationsRoute(
                factory = notificationsViewModelFactory,
                onGroupsClick = { destination = EvenDestination.GroupList },
                onProfileClick = { destination = EvenDestination.Login() },
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        }
    }
}
