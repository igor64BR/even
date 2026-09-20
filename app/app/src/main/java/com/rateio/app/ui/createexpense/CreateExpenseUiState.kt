package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Participant
import java.time.LocalDate

/**
 * Estado do formulário "Nova despesa" (T24.1/T24.2, estendido em T26 para os três modos de
 * divisão). [splitMode] é a aba ativa em "Como dividir"
 * ([com.rateio.app.ui.createexpense.SplitTypeTabs]) — Igual (T24), Percentual (T26.1) e Valor fixo
 * (T26.2), todas habilitadas.
 *
 * [participants] alimenta o seletor de "Quem pagou" (reaproveita [Participant] de `:domain`
 * direto, sem UI model próprio — não há nada a adaptar além do nome). [splitRows] nasce com todos
 * os participantes marcados (mesma regra do protótipo, `prototype/nova-despesa.html`:
 * `incluidos = new Set(group.participantes.map(p => p.id))`), com [ExpenseSplitRowUiModel
 * .amountCents] recalculado ao vivo pro modo Igual e [ExpenseSplitRowUiModel.percentageInput]
 * semeado com o percentual padrão (`Math.round(100 / n)`) — ver [CreateExpenseViewModel].
 *
 * [participantsError] cobre só o modo Igual ("selecione pelo menos 1 participante" — igual antes
 * de T26). Percentual/Valor fixo não têm um flag de erro próprio: a soma é sempre visível ao vivo
 * nas listas daquelas abas ([PercentageSplitList]/[FixedAmountSplitList], mesmo `#split-sum`
 * sempre-ligado do protótipo), e `onSaveClick` bloqueia sem persistir quando a soma não fecha
 * (ver [CreateExpenseViewModel.validate]).
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
)

/**
 * Uma linha de `#split-area`, com os campos dos três modos coexistindo (só um é mostrado por vez,
 * de acordo com [CreateExpenseUiState.splitMode]): [isIncluded]/[amountCents] pro modo Igual (T24),
 * [percentageInput] pro modo Percentual (T26.1), [fixedAmountInput] pro modo Valor fixo (T26.2).
 * Guardar os três juntos (em vez de um estado por aba) evita perder o que o usuário já digitou
 * numa aba ao só espiar outra — mesmo comportamento do protótipo (`percentuais`/`fixos` são mapas
 * que sobrevivem à troca de aba).
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

/** Evento de navegação, emitido depois que a despesa é persistida no Room. */
sealed interface CreateExpenseEvent {
    data object ExpenseCreated : CreateExpenseEvent
}
