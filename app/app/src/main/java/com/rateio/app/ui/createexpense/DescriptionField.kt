package com.rateio.app.ui.createexpense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rateio.app.ui.theme.LocalRateioColors

/** `#field-desc` do protótipo: descrição da despesa + erro "Descreve a despesa." quando vazia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionField(
    description: String,
    isError: Boolean,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "Descrição") },
        placeholder = { Text(text = "Ex.: Jantar de sexta") },
        isError = isError,
        supportingText = errorTextOrNull(isError, "Descreve a despesa."),
        singleLine = true,
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
