package com.rateio.app.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.format.formatCentsAsBrl
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * The prototype's `.balance`: a small label ("owed to you"/"you owe") over the value, colored
 * according to the balance — green (credit), terracotta (owed), neutral (settled). "Settled up"
 * drops the label and shows only the word, same as `index.html`.
 */
@Composable
fun GroupBalanceLabel(balance: GroupBalance, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    val (color, label, value) = when (balance) {
        is GroupBalance.Settled -> Triple(colors.neutral, null, "settled up")
        is GroupBalance.YouAreOwed -> Triple(colors.credit, "owed to you", formatCentsAsBrl(balance.amountCents))
        is GroupBalance.YouOwe -> Triple(colors.owed, "you owe", formatCentsAsBrl(balance.amountCents))
    }

    CompositionLocalProvider(LocalContentColor provides color) {
        Column(modifier = modifier) {
            if (label != null) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
            }
            Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
        }
    }
}
