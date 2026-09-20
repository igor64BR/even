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
 * `#tabs` do protótipo ("Igual / Percentual / Valor fixo"). T24 só implementava a divisão Igual —
 * as outras duas apareciam desabilitadas; T26 troca isso por abas de verdade, todas habilitadas,
 * trocando [CreateExpenseUiState.splitMode] via [onModeSelected].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTypeTabs(
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SplitModeChip(
            label = "Igual",
            mode = SplitMode.EQUAL,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
        SplitModeChip(
            label = "Percentual",
            mode = SplitMode.PERCENTAGE,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
        SplitModeChip(
            label = "Valor fixo",
            mode = SplitMode.FIXED_AMOUNT,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitModeChip(
    label: String,
    mode: SplitMode,
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selectedMode == mode,
        enabled = true,
        onClick = { onModeSelected(mode) },
        label = { Text(text = label) },
        modifier = modifier,
    )
}
