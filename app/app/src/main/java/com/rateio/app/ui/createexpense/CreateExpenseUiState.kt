package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Participant
import java.time.LocalDate

/**
 * Estado do formulário "Nova despesa" (T24.1/T24.2) — só o modo de divisão Igual é funcional
 * nesta task; percentual/valor fixo (T26) aparecem como abas desabilitadas na tela
 * ([com.rateio.app.ui.createexpense.SplitTypeTabs]).
 *
 * [participants] alimenta o seletor de "Quem pagou" (reaproveita [Participant] de `:domain`
 * direto, sem UI model próprio — não há nada a adaptar além do nome). [splitRows] nasce com todos
 * os participantes marcados (mesma regra do protótipo, `prototype/nova-despesa.html`:
 * `incluidos = new Set(group.participantes.map(p => p.id))`), e cada
 * [ExpenseSplitRowUiModel.amountCents] é recalculado ao vivo a cada mudança de valor total ou de
 * seleção — ver [CreateExpenseViewModel].
 */
data class CreateExpenseUiState(
    val description: String = "",
    val amountInput: String = "",
    val payerId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val participants: List<Participant> = emptyList(),
    val splitRows: List<ExpenseSplitRowUiModel> = emptyList(),
    val descriptionError: Boolean = false,
    val amountError: Boolean = false,
    val participantsError: Boolean = false,
    val isSaving: Boolean = false,
)

/** Uma linha de `#split-area` na aba Igual: checkbox de inclusão + valor calculado ao vivo. */
data class ExpenseSplitRowUiModel(
    val participantId: String,
    val name: String,
    val isYou: Boolean,
    val isIncluded: Boolean,
    val amountCents: Long = 0L,
)

/** Evento de navegação, emitido depois que a despesa é persistida no Room. */
sealed interface CreateExpenseEvent {
    data object ExpenseCreated : CreateExpenseEvent
}
