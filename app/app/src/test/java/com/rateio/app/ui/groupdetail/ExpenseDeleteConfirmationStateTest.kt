package com.rateio.app.ui.groupdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers T29.2: "the delete confirmation really asks for confirmation before deleting (doesn't
 * delete on the first tap)" — the whole rule lives in [ExpenseDeleteConfirmationState] (extracted
 * from `GroupDetailScreen.ExpenseList` precisely to make it testable without setting up Compose),
 * so it can be verified here with plain JUnit, no Robolectric.
 */
class ExpenseDeleteConfirmationStateTest {

    @Test
    fun `requesting a delete only stores the pending id, doesn't call any callback`() {
        val state = ExpenseDeleteConfirmationState()

        state.request("e1")

        assertEquals("e1", state.pendingExpenseId)
    }

    @Test
    fun `confirming without having requested first doesn't call the callback`() {
        val state = ExpenseDeleteConfirmationState()
        var called = false

        state.confirm { called = true }

        assertTrue("with no pending deletion, confirm() shouldn't do anything", !called)
    }

    @Test
    fun `confirming after requesting calls the callback with the right id and clears the pending one`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")
        var deletedId: String? = null

        state.confirm { deletedId = it }

        assertEquals("e1", deletedId)
        assertNull("dialog closes after confirming", state.pendingExpenseId)
    }

    @Test
    fun `cancelling after requesting clears the pending id without calling the callback`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")
        var called = false

        state.dismiss()

        assertNull(state.pendingExpenseId)
        state.confirm { called = true }
        assertTrue("cancelling doesn't leave a pending deletion to confirm later", !called)
    }

    @Test
    fun `requesting a delete for another expense swaps the pending one without deleting the previous one`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")

        state.request("e2")

        assertEquals("e2", state.pendingExpenseId)
    }
}
