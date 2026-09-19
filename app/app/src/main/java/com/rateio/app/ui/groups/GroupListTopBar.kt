package com.rateio.app.ui.groups

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `.topbar h1` do protótipo — título alinhado à esquerda (o CSS não centraliza, `h1 { flex: 1 }`
 * com um único filho), sem botão de voltar (é a tela raiz da árvore de navegação).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListTopBar() {
    val colors = LocalRateioColors.current
    TopAppBar(
        title = { Text(text = "Seus grupos", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.paper,
            titleContentColor = colors.ink,
        ),
    )
}
