package com.tally.app.ui.createexpense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.ui.format.formatCentsAsBrl
import com.tally.app.ui.theme.LocalTallyColors

/**
 * The prototype's `#split-area` in the Equal tab: one row per participant, an inclusion checkbox +
 * the live-calculated amount ([ExpenseSplitRowUiModel.amountCents], already closed out by the
 * largest-remainder rule — see [calculateEqualSplit]). No calculation lives here, only
 * presentation (T24, Object Calisthenics/Clean Code).
 */
@Composable
fun ParticipantSplitList(
    rows: List<ExpenseSplitRowUiModel>,
    isError: Boolean,
    onParticipantToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            ParticipantSplitRow(row = row, onToggle = { onParticipantToggled(row.participantId) })
        }
        if (isError) {
            Text(
                text = "Select at least 1 participant.",
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ParticipantSplitRow(row: ExpenseSplitRowUiModel, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = row.isIncluded, onCheckedChange = { onToggle() })
            Text(text = if (row.isYou) "${row.name} (you)" else row.name)
        }
        Text(
            text = if (row.isIncluded) formatCentsAsBrl(row.amountCents) else "—",
            color = colors.inkSoft,
        )
    }
}
