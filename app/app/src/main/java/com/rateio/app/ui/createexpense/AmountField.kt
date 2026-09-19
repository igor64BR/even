package com.rateio.app.ui.createexpense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `#field-valor` do protótipo: valor total da despesa + erro "O valor precisa ser maior que
 * zero." Aceita vírgula ou ponto como decimal ([com.rateio.app.ui.format.parseAmountInputToCents]
 * faz o parsing na borda, nunca aqui).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountField(
    amountInput: String,
    isError: Boolean,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    OutlinedTextField(
        value = amountInput,
        onValueChange = onAmountChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "Valor total") },
        placeholder = { Text(text = "0,00") },
        prefix = { Text(text = "R$ ") },
        isError = isError,
        supportingText = errorTextOrNull(isError, "O valor precisa ser maior que zero."),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brand,
            unfocusedBorderColor = colors.rule,
            errorBorderColor = colors.danger,
            focusedContainerColor = colors.paperRaised,
            unfocusedContainerColor = colors.paperRaised,
        ),
    )
}

@Composable
private fun errorTextOrNull(isError: Boolean, message: String): (@Composable () -> Unit)? {
    if (!isError) return null
    return { Text(text = message) }
}
