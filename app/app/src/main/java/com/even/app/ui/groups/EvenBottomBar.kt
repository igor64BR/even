package com.even.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.even.app.ui.theme.LocalEvenColors

/** The three tabs of `.bottombar`. */
enum class EvenBottomTab { NOTIFICATIONS, GROUPS, PROFILE }

/**
 * The prototype's `.bottombar` — Notifications/Groups/Profile, the active tab in `--brand-ink`
 * (indigo drawn on paper, never the `--brand` fill — small 10.5px labels need the darker/lighter
 * variant to stay crisp), inactive ones in `--ink-soft`. [unreadNotificationsCount] > 0 draws the
 * bell's `.badge-dot` (the same `--owed` color as the prototype) — see
 * `com.even.app.ui.notifications`, the source of the count.
 *
 * [authenticatedUserName] mirrors `renderHeaderAuth` from `prototype/app.js`: `null` (signed out)
 * draws "Sign in" + a generic icon; non-null (signed in) draws "Profile" + an initials avatar, the
 * same label/icon pair `auth-slot` swaps dynamically in the prototype — never a fixed "Profile"
 * label regardless of session.
 */
@Composable
fun EvenBottomBar(
    selectedTab: EvenBottomTab,
    onTabSelected: (EvenBottomTab) -> Unit,
    unreadNotificationsCount: Int = 0,
    authenticatedUserName: String? = null,
) {
    val colors = LocalEvenColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.brandInk,
        selectedTextColor = colors.brandInk,
        unselectedIconColor = colors.inkSoft,
        unselectedTextColor = colors.inkSoft,
        indicatorColor = colors.paper,
    )

    NavigationBar(containerColor = colors.paper, contentColor = colors.inkSoft) {
        NavigationBarItem(
            selected = selectedTab == EvenBottomTab.NOTIFICATIONS,
            onClick = { onTabSelected(EvenBottomTab.NOTIFICATIONS) },
            icon = { NotificationsBellIcon(unreadCount = unreadNotificationsCount) },
            label = { Text("Notifications") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == EvenBottomTab.GROUPS,
            onClick = { onTabSelected(EvenBottomTab.GROUPS) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Groups") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == EvenBottomTab.PROFILE,
            onClick = { onTabSelected(EvenBottomTab.PROFILE) },
            icon = { ProfileTabIcon(authenticatedUserName = authenticatedUserName) },
            label = { Text(if (authenticatedUserName != null) "Profile" else "Sign in") },
            colors = itemColors,
        )
    }
}

/** An initials avatar (signed in) or a generic icon (signed out) — the same swap as `renderHeaderAuth`. */
@Composable
private fun ProfileTabIcon(authenticatedUserName: String?) {
    if (authenticatedUserName == null) {
        Icon(Icons.Filled.Person, contentDescription = null)
        return
    }
    val colors = LocalEvenColors.current
    Box(
        modifier = Modifier.size(22.dp).background(color = colors.brand, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOfProfileName(authenticatedUserName),
            color = colors.onBrand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
        )
    }
}

/** Same heuristic as `initials()` in `prototype/app.js`: up to two initials, uppercase. */
private fun initialsOfProfileName(name: String): String =
    name.split(" ")
        .filter { word -> word.isNotBlank() }
        .take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")

/** `.badge-dot` over the bell — the same `--owed` color as the prototype, only drawn when there's an unread one. */
@Composable
private fun NotificationsBellIcon(unreadCount: Int) {
    val colors = LocalEvenColors.current
    BadgedBox(
        badge = {
            if (unreadCount > 0) Badge(containerColor = colors.owed)
        },
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null)
    }
}
