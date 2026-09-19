package com.rateio.app.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rateio.app.ui.theme.LocalRateioColors

/** As três abas de `.bottombar`. A navegação entre elas é escopo de T9 — aqui só a vitrine visual. */
enum class RateioBottomTab { AVISOS, GRUPOS, PERFIL }

/**
 * `.bottombar` do protótipo — Avisos/Grupos/Perfil, aba ativa em `--ink` cheio, inativas em
 * `--ink-soft`. T8 só renderiza esta tela ("Grupos" ativa); ligar as outras duas a telas de
 * verdade é T9 (bottom nav), que esta task desbloqueia.
 */
@Composable
fun RateioBottomBar(selectedTab: RateioBottomTab, onTabSelected: (RateioBottomTab) -> Unit) {
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
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
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
