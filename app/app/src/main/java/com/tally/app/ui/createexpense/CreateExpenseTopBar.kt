package com.tally.app.ui.createexpense

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
import com.tally.app.ui.theme.LocalTallyColors
import com.tally.app.ui.theme.ThemeToggleButton

/**
 * `new-expense.html`'s `.topbar` — goes back to the group without saving. [isEditMode] (T29) only
 * swaps the title to "Edit expense"; the rest of the form is identical to create mode (T29,
 * "editing is state, not a new screen").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseTopBar(
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isEditMode: Boolean = false,
) {
    val colors = LocalTallyColors.current
    TopAppBar(
        title = { Text(text = if (isEditMode) "Edit expense" else "New expense", fontWeight = FontWeight.Bold) },
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
