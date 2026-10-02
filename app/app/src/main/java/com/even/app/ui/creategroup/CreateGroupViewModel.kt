package com.even.app.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.even.domain.model.Group
import com.even.domain.model.Participant
import com.even.domain.repository.GroupRepository
import com.even.domain.repository.ParticipantRepository
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
 * State + validation + persistence for the "New group" form. Persistence is always local
 * via [GroupRepository]/[ParticipantRepository] (Room) — zero network calls; syncing is a
 * separate, future action.
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

    /** Pressing Enter in the participant field (the prototype's `#novo-participante`) adds a chip. */
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

    /** "You" is never removed — the same rule as the prototype ([ParticipantChipUiModel.isYou]). */
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
            try {
                saveGroup(state)
            } finally {
                // Without this, `isSaving` would stay `true` forever — harmless while the screen
                // unmounted when navigating to "Your groups" right after the event below, but it
                // turned into a visible bug (a permanently disabled "Create group" button) as soon
                // as the same ViewModel was reused on a subsequent visit to this screen (see the
                // key in CreateGroupRoute). Resets BEFORE emitting the event (not after, in a
                // `finally` only around the emit either): "saving" ends when the group is
                // persisted, not when someone reacts to the navigation notification — and only this
                // way does whoever observes [events] already see `isSaving = false` at the same
                // instant the event arrives.
                _uiState.update { it.copy(isSaving = false) }
            }
            _events.emit(CreateGroupEvent.GroupCreated)
        }
    }

    /**
     * The [GroupCategory] chosen in the form isn't persisted here: `Group` (`:domain`) has no
     * category field, and adding one is a schema change out of scope here (restricted to
     * `app/app/`). Reported as a known gap, not invented.
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
