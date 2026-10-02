package com.even.app.ui.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.even.app.ui.format.formatCentsAsBrl
import com.even.app.ui.theme.LocalEvenColors

/**
 * `group.html`'s `.expense-row`: an icon, description + "who paid · date · split type", the total
 * amount on the right. Purely presentational — [expense] already comes with everything formatted
 * ([GroupDetailViewModel]).
 *
 * Two gestures, chosen as the simplest/most discoverable for each action — tapping anywhere on the
 * row calls [onClick] (opens "Edit expense" pre-filled, reused in edit mode); the trash icon calls
 * [onDeleteClick], which only *asks* for confirmation — [GroupDetailScreen] is the one that decides
 * to actually delete after the dialog, [ExpenseRow] never deletes anything on its own.
 */
@Composable
fun ExpenseRow(
    expense: ExpenseRowUiModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // `.expense-mark`: the amber money tile — `--accent-bg` pairs only with `--accent-ink`.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = colors.accentBg, shape = RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.AttachMoney, contentDescription = null, tint = colors.accentInk)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.description, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.ink)
            Text(
                text = "${expense.payerName} paid · ${expense.dateLabel} · ${expense.splitTypeLabel}",
                fontSize = 12.sp,
                color = colors.inkSoft,
            )
        }
        Text(
            text = formatCentsAsBrl(expense.amountCents),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.ink,
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete expense",
                tint = colors.inkSoft,
            )
        }
    }
}
