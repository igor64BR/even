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
 * `#split-area` do protótipo na aba Percentual: uma linha por participante com input de `%` (sem
 * checkbox — todo participante entra na divisão percentual, igual ao protótipo) + `#split-sum`
 * sempre visível com a soma corrente, verde quando fecha 100%, vermelho quando não fecha (T26.1).
 * Nenhum cálculo mora aqui — [sumPercentages]/[isPercentageSplitComplete] são as mesmas funções
 * puras usadas pra montar `ExpenseSplit.Weight` ao salvar ([CreateExpenseViewModel]).
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
            text = "soma: $sum%",
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
        Text(text = if (row.isYou) "${row.name} (você)" else row.name)
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
