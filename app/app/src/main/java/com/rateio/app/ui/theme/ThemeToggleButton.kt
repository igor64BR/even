package com.rateio.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * Sun/moon button for the `TopAppBar` — a global component (same behavior on every screen, see
 * `prototype/app.js`'s `initThemeToggle`, called from every `.topbar` in the prototype). The icon
 * always shows the ACTION the tap performs, never the current state: sun visible = "tap to
 * lighten" (dark theme active), moon visible = "tap to darken" (light theme active) — never the
 * other way around.
 */
@Composable
fun ThemeToggleButton(isDarkTheme: Boolean, onToggleClick: () -> Unit) {
    val label = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
    IconButton(onClick = onToggleClick) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = label,
        )
    }
}
