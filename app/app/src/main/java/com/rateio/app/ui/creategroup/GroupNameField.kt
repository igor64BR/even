package com.rateio.app.ui.creategroup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rateio.app.ui.theme.LocalRateioColors

/** The prototype's `#field-nome`: a text input + the "Give the group a name." error when empty. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupNameField(
    name: String,
    isError: Boolean,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = modifier,
        label = { Text(text = "Group name") },
        placeholder = { Text(text = "E.g.: Saturday barbecue") },
        isError = isError,
        supportingText = errorTextOrNull(isError, "Give the group a name."),
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

/** The error Composable only shows up when there's an error — the same visual rule as `.field .error`. */
@Composable
private fun errorTextOrNull(isError: Boolean, message: String): (@Composable () -> Unit)? {
    if (!isError) return null
    return { Text(text = message) }
}
