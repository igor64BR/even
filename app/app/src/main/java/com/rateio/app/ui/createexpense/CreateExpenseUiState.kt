package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Participant
import java.time.LocalDate

/**
 * State of the "New expense" form (T24.1/T24.2, extended in T26 for the three split modes).
 * [splitMode] is the active tab in "How to split"
 * ([com.rateio.app.ui.createexpense.SplitTypeTabs]) — Equal (T24), Percentage (T26.1) and Fixed
 * amount (T26.2), all enabled.
 *
 * [participants] feeds the "Who paid" selector (reuses [Participant] from `:domain` directly, no
 * dedicated UI model — there's nothing to adapt besides the name). [splitRows] starts with every
 * participant checked (the same rule as the prototype, `prototype/new-expense.html`:
 * `incluidos = new Set(group.participantes.map(p => p.id))`), with
 * [ExpenseSplitRowUiModel.amountCents] recalculated live for the Equal mode and
 * [ExpenseSplitRowUiModel.percentageInput] seeded with the default percentage
 * (`Math.round(100 / n)`) — see [CreateExpenseViewModel].
 *
 * [participantsError] only covers the Equal mode ("select at least 1 participant" — same as
 * before T26). Percentage/Fixed amount have no error flag of their own: the sum is always visible
 * live in those tabs' lists ([PercentageSplitList]/[FixedAmountSplitList], the same always-on
 * `#split-sum` from the prototype), and `onSaveClick` blocks without persisting when the sum
 * doesn't add up (see [CreateExpenseViewModel.validate]).
 *
 * [expenseId] is `null` in create mode (T24) and the id of the expense being edited in edit mode
 * (T29) — the same form for both modes ("editing is state, not a new screen",
 * T29-app-editar-excluir-despesa.md), only what `onSaveClick` does with the result changes (insert
 * vs. update) and the text [com.rateio.app.ui.createexpense.CreateExpenseScreen]/
 * [CreateExpenseTopBar] show.
 */
data class CreateExpenseUiState(
    val description: String = "",
    val amountInput: String = "",
    val payerId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val participants: List<Participant> = emptyList(),
    val splitMode: SplitMode = SplitMode.EQUAL,
    val splitRows: List<ExpenseSplitRowUiModel> = emptyList(),
    val descriptionError: Boolean = false,
    val amountError: Boolean = false,
    val participantsError: Boolean = false,
    val isSaving: Boolean = false,
    val expenseId: String? = null,
) {
    /** `true` only when the form is pre-filled with an existing expense (T29.1). */
    val isEditMode: Boolean get() = expenseId != null
}

/**
 * A row of `#split-area`, with the fields of all three modes coexisting (only one is shown at a
 * time, based on [CreateExpenseUiState.splitMode]): [isIncluded]/[amountCents] for Equal mode
 * (T24), [percentageInput] for Percentage mode (T26.1), [fixedAmountInput] for Fixed amount mode
 * (T26.2). Keeping all three together (instead of one state per tab) avoids losing what the user
 * already typed in a tab just by peeking at another — the same behavior as the prototype
 * (`percentuais`/`fixos` are maps that survive tab switching).
 */
data class ExpenseSplitRowUiModel(
    val participantId: String,
    val name: String,
    val isYou: Boolean,
    val isIncluded: Boolean,
    val amountCents: Long = 0L,
    val percentageInput: String = "",
    val fixedAmountInput: String = "",
)

/**
 * A navigation event, emitted after the expense is persisted to Room — the same event for both
 * creation (T24) and editing (T29), since both go back to "Group details" the same way and the
 * recomputed balance gets there via a reactive `Flow` (no data needs to be carried in the event).
 */
sealed interface CreateExpenseEvent {
    data object Saved : CreateExpenseEvent
}
