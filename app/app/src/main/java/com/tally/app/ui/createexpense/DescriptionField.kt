package com.tally.app.ui.createexpense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.LocalTallyColors

/** The prototype's `#field-desc`: the expense's description + the "Describe the expense." error when empty. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionField(
    description: String,
    isError: Boolean,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "Description") },
        placeholder = { Text(text = "E.g.: Friday dinner") },
        isError = isError,
        supportingText = errorTextOrNull(isError, "Describe the expense."),
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
