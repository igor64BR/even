package com.tally.app.ui.createexpense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.LocalTallyColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The prototype's `#data`: a date field (defaults to today), opens the picker when the icon is tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, onDateSelected: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current
    var isPickerOpen by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date.format(DISPLAY_FORMATTER),
        onValueChange = {},
        readOnly = true,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = "Date") },
        trailingIcon = {
            IconButton(onClick = { isPickerOpen = true }) {
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Pick a date")
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.brand,
            unfocusedBorderColor = colors.rule,
            focusedContainerColor = colors.paperRaised,
            unfocusedContainerColor = colors.paperRaised,
        ),
    )

    if (isPickerOpen) {
        DatePickerHost(
            initialDate = date,
            onConfirm = { pickedDate ->
                onDateSelected(pickedDate)
                isPickerOpen = false
            },
            onDismiss = { isPickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerHost(initialDate: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochMillisUtc())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let { millis -> onConfirm(millis.toLocalDateUtc()) } }) {
                Text(text = "OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

private fun LocalDate.toEpochMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
