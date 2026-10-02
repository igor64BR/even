package com.even.app.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.even.app.ui.format.formatCentsAsBrl
import com.even.app.ui.theme.LocalEvenColors

/**
 * The prototype's `.balance`: a small label ("owed to you"/"you owe") over the value. Only the
 * *value* carries the balance colour — `--credit` (green), `--owed` (coral) or `--neutral` (settled
 * up, deliberately colourless); `.balance .label` is plain `--ink-soft` in the CSS. "Settled up"
 * drops the label and shows only the word, same as `index.html`.
 */
@Composable
fun GroupBalanceLabel(balance: GroupBalance, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    val (color, label, value) = when (balance) {
        is GroupBalance.Settled -> Triple(colors.neutral, null, "settled up")
        is GroupBalance.YouAreOwed -> Triple(colors.credit, "owed to you", formatCentsAsBrl(balance.amountCents))
        is GroupBalance.YouOwe -> Triple(colors.owed, "you owe", formatCentsAsBrl(balance.amountCents))
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = colors.inkSoft,
            )
        }
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = color)
    }
}
