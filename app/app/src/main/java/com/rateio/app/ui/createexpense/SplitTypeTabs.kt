package com.rateio.app.ui.createexpense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * `#tabs` do protótipo ("Igual / Percentual / Valor fixo"). T24 só implementa a divisão Igual
 * (T24.2) — as outras duas aparecem desabilitadas, como a própria task prevê ("podem aparecer
 * desabilitadas ou ocultas"); T26 troca isso por abas de verdade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTypeTabs(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = true,
            enabled = true,
            onClick = {},
            label = { Text(text = "Igual") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = false,
            enabled = false,
            onClick = {},
            label = { Text(text = "Percentual") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = false,
            enabled = false,
            onClick = {},
            label = { Text(text = "Valor fixo") },
            modifier = Modifier.weight(1f),
        )
    }
}
