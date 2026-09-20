package com.rateio.app.ui.groupdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre T29.2: "confirmação de exclusão realmente pede confirmação antes de excluir (não exclui
 * no primeiro toque)" — a regra vive inteira em [ExpenseDeleteConfirmationState] (extraída de
 * `GroupDetailScreen.ExpenseList` justamente pra ficar testável sem montar Compose), então dá pra
 * verificar aqui com JUnit puro, sem Robolectric.
 */
class ExpenseDeleteConfirmationStateTest {

    @Test
    fun `pedir exclusao so guarda o id pendente, nao chama callback nenhum`() {
        val state = ExpenseDeleteConfirmationState()

        state.request("e1")

        assertEquals("e1", state.pendingExpenseId)
    }

    @Test
    fun `confirmar sem ter pedido antes nao chama o callback`() {
        val state = ExpenseDeleteConfirmationState()
        var chamado = false

        state.confirm { chamado = true }

        assertTrue("sem exclusao pendente, confirm() nao deve fazer nada", !chamado)
    }

    @Test
    fun `confirmar depois de pedir chama o callback com o id certo e limpa o pendente`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")
        var idExcluido: String? = null

        state.confirm { idExcluido = it }

        assertEquals("e1", idExcluido)
        assertNull("diálogo fecha depois de confirmar", state.pendingExpenseId)
    }

    @Test
    fun `cancelar depois de pedir limpa o pendente sem chamar o callback`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")
        var chamado = false

        state.dismiss()

        assertNull(state.pendingExpenseId)
        state.confirm { chamado = true }
        assertTrue("cancelar nao deixa uma exclusao pendente pra confirmar depois", !chamado)
    }

    @Test
    fun `pedir exclusao de outra despesa troca o pendente sem excluir a anterior`() {
        val state = ExpenseDeleteConfirmationState()
        state.request("e1")

        state.request("e2")

        assertEquals("e2", state.pendingExpenseId)
    }
}
