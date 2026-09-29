package com.tally.app.ui.createexpense

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
 * The prototype's `#tabs` ("Equal / Percentage / Fixed amount"). T24 only implemented the Equal
 * split — the other two showed up disabled; T26 swaps that for real tabs, all enabled, changing
 * [CreateExpenseUiState.splitMode] via [onModeSelected].
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
            label = "Equal",
            mode = SplitMode.EQUAL,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
        SplitModeChip(
            label = "Percentage",
            mode = SplitMode.PERCENTAGE,
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
        SplitModeChip(
            label = "Fixed amount",
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
