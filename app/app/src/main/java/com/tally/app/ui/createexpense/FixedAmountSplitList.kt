package com.tally.app.ui.createexpense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.ui.format.formatCentsAsBrl
import com.tally.app.ui.theme.LocalTallyColors
import com.tally.domain.model.Money

/**
 * The prototype's `#split-area` in the Fixed amount tab: one row per participant with an R$ input
 * (no checkbox — whoever is left at 0/empty simply doesn't take part in the expense, see
 * [buildSplits]) + an always-visible `#split-sum` showing "sum: X of Y", green when it matches the
 * total, red when it doesn't. No calculation lives here — [sumFixedAmounts]/
 * [isFixedAmountSplitComplete] are the same pure functions used to build `ExpenseSplit.FixedAmount`
 * on save ([CreateExpenseViewModel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedAmountSplitList(
    rows: List<ExpenseSplitRowUiModel>,
    total: Money,
    onFixedAmountChanged: (participantId: String, fixedAmountInput: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            FixedAmountSplitRow(
                row = row,
                onFixedAmountChanged = { value -> onFixedAmountChanged(row.participantId, value) },
            )
        }
        val sum = sumFixedAmounts(rows)
        val isComplete = isFixedAmountSplitComplete(rows, total)
        Text(
            text = "sum: ${formatCentsAsBrl(sum.cents)} of ${formatCentsAsBrl(total.cents)}",
            color = if (isComplete) colors.credit else colors.danger,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedAmountSplitRow(
    row: ExpenseSplitRowUiModel,
    onFixedAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = if (row.isYou) "${row.name} (you)" else row.name)
        OutlinedTextField(
            value = row.fixedAmountInput,
            onValueChange = onFixedAmountChanged,
            modifier = Modifier.width(120.dp),
            placeholder = { Text(text = "0,00") },
            prefix = { Text(text = "R$ ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brand,
                unfocusedBorderColor = colors.rule,
                focusedContainerColor = colors.paperRaised,
                unfocusedContainerColor = colors.paperRaised,
            ),
        )
    }
}
