package com.rateio.app.ui.creategroup

import java.util.UUID

/**
 * Estado do formulário "Novo grupo" (T16.1/T16.2). [participants] sempre começa com "Você" fixo
 * ([ParticipantChipUiModel.you], não removível — mesma regra de `prototype/criar-grupo.html`).
 *
 * [category] é seleção só de UI: `Group` (`:domain`, T7B) não tem campo de categoria hoje, então
 * esta escolha ainda não é persistida — ver nota em
 * [com.rateio.app.ui.creategroup.CreateGroupViewModel.saveGroup]. Mesmo tipo de lacuna documentada
 * que T8 deixou para `Group.isSynced` antes de T7B resolver, desta vez fora do escopo de T16
 * (restrita a `app/app/`, sem mexer em `:domain`).
 */
data class CreateGroupUiState(
    val name: String = "",
    val category: GroupCategory = GroupCategory.VIAGEM,
    val participants: List<ParticipantChipUiModel> = listOf(ParticipantChipUiModel.you()),
    val newParticipantName: String = "",
    val nameError: Boolean = false,
    val participantsError: Boolean = false,
    val isSaving: Boolean = false,
)

/** Réplica das opções de `#categorias` em `criar-grupo.html`, "Viagem" selecionada por padrão. */
enum class GroupCategory(val label: String) {
    VIAGEM("Viagem"),
    REPUBLICA("República"),
    CHURRASCO("Churrasco"),
    OUTRO("Outro"),
}

/** Um chip de participante no formulário. [isYou] marca o dono do dispositivo — fixo, não-removível. */
data class ParticipantChipUiModel(
    val id: String,
    val name: String,
    val isYou: Boolean = false,
) {
    companion object {
        private const val YOU_ID = "voce"

        fun you() = ParticipantChipUiModel(id = YOU_ID, name = "Você", isYou = true)

        fun named(name: String) = ParticipantChipUiModel(id = UUID.randomUUID().toString(), name = name)
    }
}

/** Evento de navegação, emitido depois que o grupo é persistido no Room. */
sealed interface CreateGroupEvent {
    data object GroupCreated : CreateGroupEvent
}
