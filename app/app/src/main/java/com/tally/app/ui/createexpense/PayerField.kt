package com.tally.app.ui.createexpense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.LocalTallyColors
import com.tally.domain.model.Participant

/** The prototype's `#pagador`: a "Who paid" selector among the group's participants. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayerField(
    participants: List<Participant>,
    selectedPayerId: String?,
    onPayerSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTallyColors.current
    var isExpanded by remember { mutableStateOf(false) }
    val selectedLabel = participants.firstOrNull { it.id == selectedPayerId }?.let(::payerLabel).orEmpty()

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Who paid") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brand,
                unfocusedBorderColor = colors.rule,
                focusedContainerColor = colors.paperRaised,
                unfocusedContainerColor = colors.paperRaised,
            ),
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            participants.forEach { participant ->
                DropdownMenuItem(
                    text = { Text(text = payerLabel(participant)) },
                    onClick = {
                        onPayerSelected(participant.id)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

private fun payerLabel(participant: Participant): String =
    if (participant.isYou) "${participant.name} (you)" else participant.name
