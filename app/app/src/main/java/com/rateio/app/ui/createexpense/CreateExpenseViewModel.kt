package com.rateio.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.app.ui.format.parseAmountInputToCents
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Money
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.ParticipantRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado + cálculo de divisão + persistência do formulário "Nova despesa" (T24), para um
 * [groupId] já existente (grupo local ou sincronizado — não importa aqui, ver nota em
 * [saveExpense]). Participantes vêm de [participantRepository] (Room, T7B); a divisão Igual usa
 * [calculateEqualSplit] (função pura, testada isoladamente) em vez de `/` inteiro ingênuo, pra
 * fechar centavo igual ao motor (`algorithm-spec.md`). Persistência é sempre local via
 * [expenseRepository] — zero chamada de rede (constitution.md, princípio 1).
 */
class CreateExpenseViewModel(
    private val groupId: String,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateExpenseEvent>()
    val events: SharedFlow<CreateExpenseEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val participants = participantRepository.getParticipantsFlow(groupId).first()
            _uiState.update { it.withParticipants(participants) }
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description.take(MAX_DESCRIPTION_LENGTH), descriptionError = false) }
    }

    fun onAmountChanged(amountInput: String) {
        _uiState.update { it.copy(amountInput = amountInput, amountError = false).withRecalculatedSplit() }
    }

    fun onPayerSelected(payerId: String) {
        _uiState.update { it.copy(payerId = payerId) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    /** Marca/desmarca um participante da divisão (`data-id` do checkbox no protótipo). */
    fun onParticipantToggled(participantId: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(isIncluded = !row.isIncluded) else row
                },
            ).withRecalculatedSplit()
        }
    }

    /** Troca a aba ativa em "Como dividir" (T26). `participantsError` só faz sentido no modo Igual
     * — some ao trocar de aba, igual o protótipo troca `#split-area` sem carregar erro da aba anterior. */
    fun onSplitModeSelected(mode: SplitMode) {
        _uiState.update { it.copy(splitMode = mode, participantsError = false) }
    }

    /** `data-id` do input de `%` na aba Percentual (T26.1). */
    fun onPercentageChanged(participantId: String, percentageInput: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(percentageInput = percentageInput) else row
                },
            )
        }
    }

    /** `data-id` do input de R$ na aba Valor fixo (T26.2). */
    fun onFixedAmountChanged(participantId: String, fixedAmountInput: String) {
        _uiState.update { state ->
            state.copy(
                splitRows = state.splitRows.map { row ->
                    if (row.participantId == participantId) row.copy(fixedAmountInput = fixedAmountInput) else row
                },
            )
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = state.validate()
        _uiState.update {
            it.copy(
                descriptionError = !validation.descriptionValid,
                amountError = !validation.amountValid,
                // Só o modo Igual usa esse flag pra pintar a lista de participantes de erro — nos
                // outros dois modos a soma inválida já é visível ao vivo em #split-sum, não precisa
                // de um segundo aviso (ver nota de classe de CreateExpenseUiState).
                participantsError = state.splitMode == SplitMode.EQUAL && !validation.splitValid,
            )
        }
        if (!validation.isValid) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveExpense(state, validation.amountCents)
            _events.emit(CreateExpenseEvent.ExpenseCreated)
        }
    }

    /**
     * Lacuna conhecida, documentada (não implementada por conta própria): se [groupId] pertence a
     * um grupo já sincronizado (`Group.isSynced`), esta task não envia a despesa ao backend — só
     * persiste local. Enviar despesas de grupos sincronizados ao servidor é extensão futura de T19
     * (mesmo precedente de "Sincronizar este grupo"), fora do escopo de T24.
     */
    private suspend fun saveExpense(state: CreateExpenseUiState, amountCents: Long) {
        val payerId = requireNotNull(state.payerId) { "Nenhum pagador selecionado pro grupo $groupId." }
        val splits = buildSplits(state.splitMode, state.splitRows)

        expenseRepository.insertExpense(
            Expense(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                description = state.description.trim(),
                amountCents = amountCents,
                paidByParticipantId = payerId,
                createdAt = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                splits = splits,
            ),
        )
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 40
    }
}

/**
 * Participantes recém-carregados do Room: todos entram na divisão, pagador default é "Você". O
 * percentual default (`Math.round(100 / n)`, [defaultPercentage]) é semeado igual ao protótipo —
 * o valor fixo fica vazio (não existe "valor fixo padrão" sensato pra propor).
 */
private fun CreateExpenseUiState.withParticipants(participants: List<Participant>): CreateExpenseUiState {
    val defaultPayerId = participants.firstOrNull { it.isYou }?.id ?: participants.firstOrNull()?.id
    val defaultPercentageInput = defaultPercentage(participants.size).toString()
    val rows = participants.map { participant ->
        ExpenseSplitRowUiModel(
            participantId = participant.id,
            name = participant.name,
            isYou = participant.isYou,
            isIncluded = true,
            percentageInput = defaultPercentageInput,
        )
    }
    return copy(participants = participants, payerId = defaultPayerId, splitRows = rows).withRecalculatedSplit()
}

/** Recalcula [ExpenseSplitRowUiModel.amountCents] de cada linha marcada (T24.2, cálculo ao vivo). */
private fun CreateExpenseUiState.withRecalculatedSplit(): CreateExpenseUiState {
    val totalCents = parseAmountInputToCents(amountInput) ?: 0L
    val includedIds = splitRows.filter { it.isIncluded }.map { it.participantId }
    val parts = calculateEqualSplit(Money.ofCents(totalCents), includedIds)

    return copy(
        splitRows = splitRows.map { row -> row.copy(amountCents = parts[row.participantId]?.cents ?: 0L) },
    )
}

private data class FormValidation(
    val descriptionValid: Boolean,
    val amountValid: Boolean,
    val splitValid: Boolean,
    val amountCents: Long,
) {
    val isValid: Boolean get() = descriptionValid && amountValid && splitValid
}

/**
 * T24.4 + T26.1/T26.2: descrição obrigatória, valor > 0, e a divisão precisa fechar de acordo com
 * a aba ativa — pelo menos 1 participante selecionado no modo Igual, soma = 100% no Percentual
 * (ver [isPercentageSplitComplete]), soma = valor total no Valor fixo (ver
 * [isFixedAmountSplitComplete]).
 */
private fun CreateExpenseUiState.validate(): FormValidation {
    val amountCents = parseAmountInputToCents(amountInput) ?: 0L
    val splitValid = when (splitMode) {
        SplitMode.EQUAL -> splitRows.any { it.isIncluded }
        SplitMode.PERCENTAGE -> isPercentageSplitComplete(splitRows)
        SplitMode.FIXED_AMOUNT -> isFixedAmountSplitComplete(splitRows, Money.ofCents(amountCents))
    }
    return FormValidation(
        descriptionValid = description.isNotBlank(),
        amountValid = amountCents > 0,
        splitValid = splitValid,
        amountCents = amountCents,
    )
}

/**
 * Traduz [rows] pro subtipo [ExpenseSplit] certo de acordo com [mode] (T26.3: "ao salvar, constrói
 * Weight/FixedAmount pros participantes selecionados").
 * - Igual: só os participantes marcados, [ExpenseSplit.Equal] — inalterado desde T24.
 * - Percentual: todos os participantes (não há checkbox nessa aba, igual ao protótipo), peso =
 *   percentual digitado (0 se a entrada estiver vazia/inválida — só chega aqui se a soma já fechou
 *   100%, então "vazio" não deveria sobrar, mas não há por que persistir `null` como estado
 *   inválido em vez de 0).
 * - Valor fixo: só participantes com valor digitado e positivo (`fixos[p.id] > 0` no protótipo) —
 *   quem ficou com 0/vazio não participa da despesa.
 */
private fun buildSplits(mode: SplitMode, rows: List<ExpenseSplitRowUiModel>): List<ExpenseSplit> =
    when (mode) {
        SplitMode.EQUAL -> rows
            .filter { it.isIncluded }
            .map { row -> ExpenseSplit.Equal(participantId = row.participantId) }

        SplitMode.PERCENTAGE -> rows.map { row ->
            ExpenseSplit.Weight(
                participantId = row.participantId,
                weight = parsePercentageInput(row.percentageInput) ?: 0L,
            )
        }

        SplitMode.FIXED_AMOUNT -> rows.mapNotNull { row ->
            val amount = parseFixedAmountInput(row.fixedAmountInput)
            if (amount != null && amount.isPositive) {
                ExpenseSplit.FixedAmount(participantId = row.participantId, amount = amount)
            } else {
                null
            }
        }
    }
