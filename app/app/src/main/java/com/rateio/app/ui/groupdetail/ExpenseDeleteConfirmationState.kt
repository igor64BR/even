package com.rateio.app.ui.groupdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * T29.2: só guarda qual despesa está com exclusão pendente de confirmação — extraído de
 * [GroupDetailScreen] (`ExpenseList`) pra a regra "o primeiro toque no ícone de lixeira só pede
 * confirmação, a exclusão de verdade só acontece depois de tocar em 'Excluir' no diálogo"
 * (T29-app-editar-excluir-despesa.md) ficar testável com JUnit puro
 * ([com.rateio.app.ui.groupdetail.ExpenseDeleteConfirmationStateTest]), sem precisar montar
 * `Compose`/Robolectric só pra verificar esse gating — nenhuma tela do projeto tem teste de UI
 * ainda (todas as `*ViewModelTest` existentes testam só o `ViewModel`, nunca a árvore de Compose).
 *
 * `mutableStateOf` (não um `MutableStateFlow`/`var` cru) porque quem usa isto é sempre um
 * Composable via `remember { ExpenseDeleteConfirmationState() }` — precisa disparar recomposição
 * quando [pendingExpenseId] muda, mas em si esta classe não depende de `Composition`/`Activity`
 * nenhuma, roda em JVM puro (por isso o teste não precisa de Robolectric).
 */
class ExpenseDeleteConfirmationState {
    var pendingExpenseId: String? by mutableStateOf(null)
        private set

    /** Ícone de lixeira de uma [ExpenseRow] tocado — só abre o diálogo, nunca exclui nada aqui. */
    fun request(expenseId: String) {
        pendingExpenseId = expenseId
    }

    /** "Cancelar" do diálogo (ou dispensado de outro jeito: toque fora, botão voltar). */
    fun dismiss() {
        pendingExpenseId = null
    }

    /**
     * "Excluir" do diálogo — só agora [onConfirmed] roda de verdade. Não faz nada sem uma
     * confirmação pendente (defensivo: o diálogo não deveria estar visível nesse caso).
     */
    fun confirm(onConfirmed: (String) -> Unit) {
        val expenseId = pendingExpenseId ?: return
        pendingExpenseId = null
        onConfirmed(expenseId)
    }
}
