package com.tally.app.ui.settledebts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.ui.format.formatCentsAsBrl
import com.tally.app.ui.theme.LocalTallyColors

/**
 * `settle.html`'s `.settle-row`: "A → B" + the highlighted amount on the left, "Mark as paid"
 * button on the right. Purely presentation — [onMarkAsPaidClick] just forwards the click, the
 * ViewModel is the one that writes the [com.tally.domain.model.Settlement].
 */
@Composable
fun SettlementSuggestionRow(
    suggestion: SettlementSuggestionRowUiModel,
    onMarkAsPaidClick: (SettlementSuggestionRowUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${suggestion.fromName} → ${suggestion.toName}", fontSize = 14.sp, color = colors.ink)
            Text(
                text = formatCentsAsBrl(suggestion.amountCents),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.ink,
            )
        }
        Button(
            onClick = { onMarkAsPaidClick(suggestion) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.paperAlt, contentColor = colors.ink),
        ) {
            Text(text = "Mark as paid", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
