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
 * `.balance` do protótipo: rótulo pequeno ("te devem"/"você deve") sobre o valor, cor conforme
 * o saldo — verde (a receber), terracota (a dever), neutro (quitado). "Quitado" some o rótulo e
 * mostra só a palavra, igual ao `index.html`.
 */
@Composable
fun GroupBalanceLabel(balance: GroupBalance, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    val (color, label, value) = when (balance) {
        is GroupBalance.Settled -> Triple(colors.neutral, null, "quitado")
        is GroupBalance.YouAreOwed -> Triple(colors.credit, "te devem", formatCentsAsBrl(balance.amountCents))
        is GroupBalance.YouOwe -> Triple(colors.owed, "você deve", formatCentsAsBrl(balance.amountCents))
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
