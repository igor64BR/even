package com.even.app.ui.groupdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.even.app.ui.theme.LocalEvenColors
import com.even.app.ui.theme.ThemeToggleButton

/** `group.html`'s `.topbar` — the title is the group's name, goes back to "Your groups". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailTopBar(
    groupName: String,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalEvenColors.current
    TopAppBar(
        title = { Text(text = groupName, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
