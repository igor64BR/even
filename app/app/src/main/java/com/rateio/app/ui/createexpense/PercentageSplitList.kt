package com.rateio.app.ui.createexpense

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
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * The prototype's `#split-area` in the Percentage tab: one row per participant with a `%` input
 * (no checkbox — every participant takes part in the percentage split, same as the prototype) + an
 * always-visible `#split-sum` with the running sum, green when it adds up to 100%, red when it
 * doesn't (T26.1). No calculation lives here — [sumPercentages]/[isPercentageSplitComplete] are the
 * same pure functions used to build `ExpenseSplit.Weight` on save ([CreateExpenseViewModel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentageSplitList(
    rows: List<ExpenseSplitRowUiModel>,
    onPercentageChanged: (participantId: String, percentageInput: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            PercentageSplitRow(
                row = row,
                onPercentageChanged = { value -> onPercentageChanged(row.participantId, value) },
            )
        }
        val sum = sumPercentages(rows)
        val isComplete = isPercentageSplitComplete(rows)
        Text(
            text = "sum: $sum%",
            color = if (isComplete) colors.credit else colors.danger,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PercentageSplitRow(
    row: ExpenseSplitRowUiModel,
    onPercentageChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = if (row.isYou) "${row.name} (you)" else row.name)
        OutlinedTextField(
            value = row.percentageInput,
            onValueChange = onPercentageChanged,
            modifier = Modifier.width(96.dp),
            suffix = { Text(text = "%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
