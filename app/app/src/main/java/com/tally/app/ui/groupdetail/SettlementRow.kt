package com.tally.app.ui.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
 * A row in "Settlement history" (T37, RF31/RF33) — the same layout as [ExpenseRow] (icon + text +
 * amount), but purely informational: a settlement already happened and can't be edited or deleted
 * from here, so there's no `onClick`/action at all, unlike [ExpenseRow].
 */
@Composable
fun SettlementRow(settlement: SettlementRowUiModel, modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = colors.paperAlt, shape = RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = colors.inkSoft)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${settlement.payerName} paid ${settlement.receiverName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = colors.ink,
            )
            Text(text = settlement.dateLabel, fontSize = 12.sp, color = colors.inkSoft)
        }
        Text(
            text = formatCentsAsBrl(settlement.amountCents),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.ink,
        )
    }
}
