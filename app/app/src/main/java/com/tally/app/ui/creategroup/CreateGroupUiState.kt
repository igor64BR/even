package com.tally.app.ui.creategroup

import java.util.UUID

/**
 * State of the "New group" form (T16.1/T16.2). [participants] always starts with a fixed "You"
 * ([ParticipantChipUiModel.you], not removable — the same rule as `prototype/create-group.html`).
 *
 * [category] is UI-only selection: `Group` (`:domain`, T7B) has no category field today, so this
 * choice isn't persisted yet — see the note in
 * [com.tally.app.ui.creategroup.CreateGroupViewModel.saveGroup]. The same kind of documented gap
 * T8 left for `Group.isSynced` before T7B resolved it, this time out of scope for T16 (restricted
 * to `app/app/`, without touching `:domain`).
 */
data class CreateGroupUiState(
    val name: String = "",
    val category: GroupCategory = GroupCategory.TRIP,
    val participants: List<ParticipantChipUiModel> = listOf(ParticipantChipUiModel.you()),
    val newParticipantName: String = "",
    val nameError: Boolean = false,
    val participantsError: Boolean = false,
    val isSaving: Boolean = false,
)

/** Mirrors the `#categorias` options in `create-group.html`, "Trip" selected by default. */
enum class GroupCategory(val label: String) {
    TRIP("Trip"),
    HOUSEHOLD("Household"),
    BARBECUE("Barbecue"),
    OTHER("Other"),
}

/** A participant chip in the form. [isYou] marks the device owner — fixed, not removable. */
data class ParticipantChipUiModel(
    val id: String,
    val name: String,
    val isYou: Boolean = false,
) {
    companion object {
        private const val YOU_ID = "you"

        fun you() = ParticipantChipUiModel(id = YOU_ID, name = "You", isYou = true)

        fun named(name: String) = ParticipantChipUiModel(id = UUID.randomUUID().toString(), name = name)
    }
}

/** A navigation event, emitted after the group is persisted to Room. */
sealed interface CreateGroupEvent {
    data object GroupCreated : CreateGroupEvent
}
