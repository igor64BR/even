package com.rateio.app.ui.creategroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `#field-part` do protótipo: chips de participante (removíveis, exceto "Você"), input que
 * adiciona no Enter/Done, erro "Precisa de pelo menos 2 participantes." e o texto de reforço
 * local-first ("Ninguém aqui precisa instalar o app nem ter conta").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsField(
    participants: List<ParticipantChipUiModel>,
    newParticipantName: String,
    isError: Boolean,
    onNewParticipantNameChange: (String) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier) {
        FieldLabel(text = "Participantes")
        ParticipantChipRow(participants = participants, onRemoveParticipant = onRemoveParticipant)
        OutlinedTextField(
            value = newParticipantName,
            onValueChange = onNewParticipantNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Nome e Enter") },
            singleLine = true,
            isError = isError,
            supportingText = if (isError) {
                { Text(text = "Precisa de pelo menos 2 participantes.") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddParticipant() }),
        )
        Text(
            text = "Só o nome. Ninguém aqui precisa instalar o app nem ter conta.",
            fontSize = 12.sp,
            color = colors.inkSoft,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantChipRow(
    participants: List<ParticipantChipUiModel>,
    onRemoveParticipant: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        participants.forEach { participant ->
            ParticipantChip(participant = participant, onRemove = onRemoveParticipant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantChip(participant: ParticipantChipUiModel, onRemove: (String) -> Unit) {
    InputChip(
        selected = false,
        onClick = {},
        label = { Text(text = participant.name) },
        trailingIcon = removeIconOrNull(participant, onRemove),
    )
}

/** "Você" não tem ícone de remoção — mesma regra do protótipo. */
@Composable
private fun removeIconOrNull(
    participant: ParticipantChipUiModel,
    onRemove: (String) -> Unit,
): (@Composable () -> Unit)? {
    if (participant.isYou) return null
    return {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remover ${participant.name}",
            modifier = Modifier
                .size(InputChipDefaults.IconSize)
                .clickable { onRemove(participant.id) },
        )
    }
}
