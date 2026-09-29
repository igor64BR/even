package com.tally.app.ui.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.ui.format.formatCentsAsBrl
import com.tally.app.ui.theme.LocalTallyColors

/**
 * `group.html`'s `.split-row`: the participant's name (+ "you" when [ParticipantBalanceUiModel.isYou])
 * on the left, a colored balance on the right ("gets back"/"owes"/"settled") — no balance
 * calculation lives here, [balance] already comes ready from the ViewModel
 * (`DebtSimplificationEngine.computeBalances`, T33).
 */
@Composable
fun ParticipantBalanceRow(participant: ParticipantBalanceUiModel, modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (participant.isYou) "${participant.name} · you" else participant.name,
            fontSize = 14.5.sp,
            color = colors.ink,
        )

        val (color, label, value) = when (val balance = participant.balance) {
            is ParticipantBalance.Settled -> Triple(colors.neutral, null, "settled")
            is ParticipantBalance.Credit -> Triple(colors.credit, "gets back", formatCentsAsBrl(balance.amountCents))
            is ParticipantBalance.Owed -> Triple(colors.owed, "owes", formatCentsAsBrl(balance.amountCents))
        }
        Column {
            if (label != null) {
                Text(text = label, fontSize = 11.sp, color = colors.inkSoft)
            }
            Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = color)
        }
    }
}
