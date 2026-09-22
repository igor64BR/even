package com.rateio.app.ui.groupdetail

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
import com.rateio.app.ui.theme.LocalRateioColors
import com.rateio.app.ui.theme.ThemeToggleButton

/** `.topbar` de `grupo.html` — título é o nome do grupo, volta pra "Seus grupos". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailTopBar(
    groupName: String,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalRateioColors.current
    TopAppBar(
        title = { Text(text = groupName, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
