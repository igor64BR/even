package com.tally.app.ui.groupdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Only holds which expense has a pending delete confirmation — extracted from
 * [GroupDetailScreen] (`ExpenseList`) so the rule "the first tap on the trash icon only asks for
 * confirmation, the actual deletion only happens after tapping 'Delete' in the dialog" is testable
 * with plain JUnit ([com.tally.app.ui.groupdetail.ExpenseDeleteConfirmationStateTest]), without
 * needing to set up `Compose`/Robolectric just to check this gating — no screen in the project has
 * a UI test yet (every existing `*ViewModelTest` only tests the `ViewModel`, never the Compose
 * tree).
 *
 * `mutableStateOf` (not a raw `MutableStateFlow`/`var`) because whoever uses this is always a
 * Composable via `remember { ExpenseDeleteConfirmationState() }` — it needs to trigger
 * recomposition when [pendingExpenseId] changes, but the class itself doesn't depend on any
 * `Composition`/`Activity`, it runs on plain JVM (which is why the test doesn't need Robolectric).
 */
class ExpenseDeleteConfirmationState {
    var pendingExpenseId: String? by mutableStateOf(null)
        private set

    /** The trash icon of an [ExpenseRow] tapped — only opens the dialog, never deletes anything here. */
    fun request(expenseId: String) {
        pendingExpenseId = expenseId
    }

    /** The dialog's "Cancel" (or dismissed another way: tap outside, back button). */
    fun dismiss() {
        pendingExpenseId = null
    }

    /**
     * The dialog's "Delete" — only now does [onConfirmed] actually run. Does nothing without a
     * pending confirmation (defensive: the dialog shouldn't be visible in that case).
     */
    fun confirm(onConfirmed: (String) -> Unit) {
        val expenseId = pendingExpenseId ?: return
        pendingExpenseId = null
        onConfirmed(expenseId)
    }
}
