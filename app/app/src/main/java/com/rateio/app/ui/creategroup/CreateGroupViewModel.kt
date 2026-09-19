package com.rateio.app.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado + validação + persistência do formulário "Novo grupo" (T16). Persistência é sempre
 * local via [GroupRepository]/[ParticipantRepository] (Room, T7) — zero chamada de rede
 * (constitution.md, princípio 1); sincronizar é ação separada e futura (T19).
 */
class CreateGroupViewModel(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateGroupEvent>()
    val events: SharedFlow<CreateGroupEvent> = _events.asSharedFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name.take(MAX_NAME_LENGTH), nameError = false) }
    }

    fun onCategorySelected(category: GroupCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNewParticipantNameChanged(name: String) {
        _uiState.update { it.copy(newParticipantName = name) }
    }

    /** Enter no campo de participante (`#novo-participante` do protótipo) adiciona um chip. */
    fun onAddParticipant() {
        val name = _uiState.value.newParticipantName.trim()
        if (name.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                participants = state.participants + ParticipantChipUiModel.named(name),
                newParticipantName = "",
                participantsError = false,
            )
        }
    }

    /** "Você" nunca é removido — mesma regra do protótipo ([ParticipantChipUiModel.isYou]). */
    fun onRemoveParticipant(participantId: String) {
        _uiState.update { state ->
            state.copy(participants = state.participants.filterNot { it.matchesRemovalTarget(participantId) })
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = state.validate()
        _uiState.update {
            it.copy(nameError = !validation.nameValid, participantsError = !validation.participantsValid)
        }
        if (!validation.isValid) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveGroup(state)
            _events.emit(CreateGroupEvent.GroupCreated)
        }
    }

    /**
     * [GroupCategory] escolhida no formulário não é persistida aqui: `Group` (`:domain`, T7B)
     * não tem campo de categoria, e adicioná-lo é mudança de schema fora do escopo de T16
     * (restrita a `app/app/`). Reportado como lacuna conhecida, não inventado.
     */
    private suspend fun saveGroup(state: CreateGroupUiState) {
        val groupId = UUID.randomUUID().toString()
        groupRepository.insertGroup(
            Group(id = groupId, name = state.name.trim(), createdAt = Instant.now()),
        )
        state.participants.forEach { participant ->
            participantRepository.insertParticipant(participant.toDomain(groupId))
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

private fun ParticipantChipUiModel.matchesRemovalTarget(targetId: String): Boolean = id == targetId && !isYou

private fun ParticipantChipUiModel.toDomain(groupId: String) = Participant(
    id = id,
    groupId = groupId,
    name = name,
    isYou = isYou,
)

private data class FormValidation(val nameValid: Boolean, val participantsValid: Boolean) {
    val isValid: Boolean get() = nameValid && participantsValid
}

private fun CreateGroupUiState.validate() = FormValidation(
    nameValid = isNameValid(name),
    participantsValid = areParticipantsValid(participants),
)

private fun isNameValid(name: String): Boolean = name.isNotBlank()

private fun areParticipantsValid(participants: List<ParticipantChipUiModel>): Boolean =
    participants.size >= MIN_PARTICIPANTS

private const val MIN_PARTICIPANTS = 2
