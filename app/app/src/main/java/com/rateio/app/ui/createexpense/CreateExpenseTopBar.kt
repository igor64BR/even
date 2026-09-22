package com.rateio.app.ui.createexpense

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

/**
 * `.topbar` de `nova-despesa.html` — volta pro grupo sem salvar. [isEditMode] (T29) troca só o
 * título pra "Editar despesa"; o resto do formulário é idêntico ao modo criação (T29, "edição é
 * estado, não tela nova").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseTopBar(
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isEditMode: Boolean = false,
) {
    val colors = LocalRateioColors.current
    TopAppBar(
        title = { Text(text = if (isEditMode) "Editar despesa" else "Nova despesa", fontWeight = FontWeight.Bold) },
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
