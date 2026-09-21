package com.rateio.app.ui.groups

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
import com.rateio.app.ui.theme.LocalRateioColors

/** As três abas de `.bottombar`. */
enum class RateioBottomTab { AVISOS, GRUPOS, PERFIL }

/**
 * `.bottombar` do protótipo — Avisos/Grupos/Perfil, aba ativa em `--ink` cheio, inativas em
 * `--ink-soft`. [unreadNotificationsCount] > 0 desenha o `.badge-dot` do sino (T41.2, mesma cor
 * `--owed` do protótipo) — ver `com.rateio.app.ui.notifications`, fonte da contagem.
 *
 * [authenticatedUserName] espelha `renderHeaderAuth` de `prototype/app.js`: `null` (deslogado)
 * desenha "Entrar" + ícone genérico; não-nulo (logado) desenha "Perfil" + avatar com iniciais,
 * mesmo par label/ícone que `auth-slot` troca dinamicamente no protótipo — nunca um rótulo
 * "Perfil" fixo independente da sessão.
 */
@Composable
fun RateioBottomBar(
    selectedTab: RateioBottomTab,
    onTabSelected: (RateioBottomTab) -> Unit,
    unreadNotificationsCount: Int = 0,
    authenticatedUserName: String? = null,
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
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Grupos") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == RateioBottomTab.PERFIL,
            onClick = { onTabSelected(RateioBottomTab.PERFIL) },
            icon = { ProfileTabIcon(authenticatedUserName = authenticatedUserName) },
            label = { Text(if (authenticatedUserName != null) "Perfil" else "Entrar") },
            colors = itemColors,
        )
    }
}

/** Avatar com iniciais (logado) ou ícone genérico (deslogado) — mesma troca de `renderHeaderAuth`. */
@Composable
private fun ProfileTabIcon(authenticatedUserName: String?) {
    if (authenticatedUserName == null) {
        Icon(Icons.Filled.Person, contentDescription = null)
        return
    }
    val colors = LocalRateioColors.current
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

/** Mesma heurística de `initials()` em `prototype/app.js`: até duas iniciais, maiúsculas. */
private fun initialsOfProfileName(name: String): String =
    name.split(" ")
        .filter { word -> word.isNotBlank() }
        .take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")

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
