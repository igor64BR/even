package com.rateio.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * Botão sol/lua do `TopAppBar` — componente global (mesma função em toda tela, ver
 * `prototype/app.js`'s `initThemeToggle`, chamada de todo `.topbar` do protótipo). O ícone mostra
 * sempre a AÇÃO do toque, nunca o estado atual: sol visível = "toque pra clarear" (tema escuro
 * ativo), lua visível = "toque pra escurecer" (tema claro ativo) — nunca o contrário.
 */
@Composable
fun ThemeToggleButton(isDarkTheme: Boolean, onToggleClick: () -> Unit) {
    val label = if (isDarkTheme) "Mudar para tema claro" else "Mudar para tema escuro"
    IconButton(onClick = onToggleClick) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = label,
        )
    }
}
