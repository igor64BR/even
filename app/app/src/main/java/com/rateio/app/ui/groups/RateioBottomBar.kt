package com.rateio.app.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
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
import com.rateio.app.ui.theme.LocalRateioColors

/** As três abas de `.bottombar`. */
enum class RateioBottomTab { AVISOS, GRUPOS, PERFIL }

/**
 * `.bottombar` do protótipo — Avisos/Grupos/Perfil, aba ativa em `--ink` cheio, inativas em
 * `--ink-soft`. [unreadNotificationsCount] > 0 desenha o `.badge-dot` do sino (T41.2, mesma cor
 * `--owed` do protótipo) — ver `com.rateio.app.ui.notifications`, fonte da contagem.
 */
@Composable
fun RateioBottomBar(
    selectedTab: RateioBottomTab,
    onTabSelected: (RateioBottomTab) -> Unit,
    unreadNotificationsCount: Int = 0,
) {
    val colors = LocalRateioColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.ink,
        selectedTextColor = colors.ink,
        unselectedIconColor = colors.inkSoft,
        unselectedTextColor = colors.inkSoft,
        indicatorColor = colors.paper,
    )

    NavigationBar(containerColor = colors.paper, contentColor = colors.inkSoft) {
        NavigationBarItem(
            selected = selectedTab == RateioBottomTab.AVISOS,
            onClick = { onTabSelected(RateioBottomTab.AVISOS) },
            icon = { NotificationsBellIcon(unreadCount = unreadNotificationsCount) },
            label = { Text("Avisos") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == RateioBottomTab.GRUPOS,
            onClick = { onTabSelected(RateioBottomTab.GRUPOS) },
            icon = { Icon(Icons.Filled.Groups, contentDescription = null) },
            label = { Text("Grupos") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == RateioBottomTab.PERFIL,
            onClick = { onTabSelected(RateioBottomTab.PERFIL) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Perfil") },
            colors = itemColors,
        )
    }
}

/** `.badge-dot` sobre o sino — mesma cor `--owed` do protótipo, só desenhado quando há não lida. */
@Composable
private fun NotificationsBellIcon(unreadCount: Int) {
    val colors = LocalRateioColors.current
    BadgedBox(
        badge = {
            if (unreadCount > 0) Badge(containerColor = colors.owed)
        },
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null)
    }
}
