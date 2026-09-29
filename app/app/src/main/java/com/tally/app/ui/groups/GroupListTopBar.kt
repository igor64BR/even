package com.tally.app.ui.groups

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.tally.app.ui.theme.LocalTallyColors
import com.tally.app.ui.theme.ThemeToggleButton

/**
 * The prototype's `.topbar h1` — title aligned to the left (the CSS doesn't center it, `h1 { flex:
 * 1 }` with a single child), no back button (it's the root screen of the navigation tree). The
 * theme button ([ThemeToggleButton]) is the `TopAppBar`'s `actions`, which Material3 already
 * pushes to the opposite side of the title — same effect as `h1 { flex: 1 }` in the prototype,
 * without needing manual layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListTopBar(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val colors = LocalTallyColors.current
    TopAppBar(
        title = { Text(text = "Your groups", fontWeight = FontWeight.Bold) },
        actions = { ThemeToggleButton(isDarkTheme = isDarkTheme, onToggleClick = onToggleTheme) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            actionIconContentColor = colors.ink,
        ),
    )
}
