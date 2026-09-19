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

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = state.validate()
        _uiState.update {
            it.copy(
                descriptionError = !validation.descriptionValid,
                amountError = !validation.amountValid,
                participantsError = !validation.participantsValid,
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
        val splits = state.splitRows
            .filter { it.isIncluded }
            .map { row -> ExpenseSplit.Equal(participantId = row.participantId) }

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

/** Participantes recém-carregados do Room: todos entram na divisão, pagador default é "Você". */
private fun CreateExpenseUiState.withParticipants(participants: List<Participant>): CreateExpenseUiState {
    val defaultPayerId = participants.firstOrNull { it.isYou }?.id ?: participants.firstOrNull()?.id
    val rows = participants.map { participant ->
        ExpenseSplitRowUiModel(
            participantId = participant.id,
            name = participant.name,
            isYou = participant.isYou,
            isIncluded = true,
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
    val participantsValid: Boolean,
    val amountCents: Long,
) {
    val isValid: Boolean get() = descriptionValid && amountValid && participantsValid
}

/** T24.4: descrição obrigatória, valor > 0, pelo menos 1 participante selecionado. */
private fun CreateExpenseUiState.validate(): FormValidation {
    val amountCents = parseAmountInputToCents(amountInput) ?: 0L
    return FormValidation(
        descriptionValid = description.isNotBlank(),
        amountValid = amountCents > 0,
        participantsValid = splitRows.any { it.isIncluded },
        amountCents = amountCents,
    )
}
