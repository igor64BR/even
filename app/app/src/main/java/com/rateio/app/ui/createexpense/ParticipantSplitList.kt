package com.rateio.app.ui.createexpense

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
import com.rateio.app.ui.format.formatCentsAsBrl
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `#split-area` do protótipo na aba Igual: uma linha por participante, checkbox de inclusão +
 * valor calculado ao vivo ([ExpenseSplitRowUiModel.amountCents], já fechado pela regra de maiores
 * restos — ver [calculateEqualSplit]). Nenhum cálculo mora aqui, só apresentação (T24, Object
 * Calisthenics/Clean Code).
 */
@Composable
fun ParticipantSplitList(
    rows: List<ExpenseSplitRowUiModel>,
    isError: Boolean,
    onParticipantToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            ParticipantSplitRow(row = row, onToggle = { onParticipantToggled(row.participantId) })
        }
        if (isError) {
            Text(
                text = "Selecione pelo menos 1 participante.",
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ParticipantSplitRow(row: ExpenseSplitRowUiModel, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = row.isIncluded, onCheckedChange = { onToggle() })
            Text(text = if (row.isYou) "${row.name} (você)" else row.name)
        }
        Text(
            text = if (row.isIncluded) formatCentsAsBrl(row.amountCents) else "—",
            color = colors.inkSoft,
        )
    }
}
