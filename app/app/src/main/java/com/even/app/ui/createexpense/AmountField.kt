package com.even.app.ui.createexpense

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
import com.even.app.ui.theme.LocalEvenColors

/**
 * The prototype's `#field-valor`: the expense's total amount + the "The amount must be greater
 * than zero." error. Accepts comma or dot as the decimal separator
 * ([com.even.app.ui.format.parseAmountInputToCents] does the parsing at the boundary, never
 * here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountField(
    amountInput: String,
    isError: Boolean,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current
    OutlinedTextField(
        value = amountInput,
        onValueChange = onAmountChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "Total amount") },
        placeholder = { Text(text = "0,00") },
        prefix = { Text(text = "R$ ") },
        isError = isError,
        supportingText = errorTextOrNull(isError, "The amount must be greater than zero."),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brandInk,
            unfocusedBorderColor = colors.controlBorder,
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
