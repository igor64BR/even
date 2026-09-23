package com.rateio.app.ui.createexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rateio.app.ui.format.formatCentsAsAmountInput
import com.rateio.app.ui.format.parseAmountInputToCents
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Money
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.ExpenseRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.ParticipantRepository
import com.rateio.domain.repository.RemoteExpenseRepository
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
 * Estado + cálculo de divisão + persistência do formulário "Nova despesa"/"Editar despesa" (T24,
 * estendido em T29 pro modo edição), para um [groupId] já existente (grupo local ou sincronizado —
 * não importa aqui, ver nota em [propagateUpdateIfSynced]). Participantes vêm de
 * [participantRepository] (Room, T7B); a divisão Igual usa [calculateEqualSplit] (função pura,
 * testada isoladamente) em vez de `/` inteiro ingênuo, pra fechar centavo igual ao motor
 * (`algorithm-spec.md`). Persistência é sempre local via [expenseRepository] — zero chamada de
 * rede bloqueante (constitution.md, princípio 1).
 *
 * [expenseId] é a única diferença de construção entre os dois modos (T29, "edição é estado, não
 * tela nova"): `null` cria uma despesa nova (T24), não-nulo pré-carrega essa despesa do Room
 * ([loadExpenseForEditing]) e faz `onSaveClick` fazer update em vez de insert
 * ([updateExistingExpense]). [groupRepository]/[remoteExpenseRepository] só entram em jogo na
 * edição, pra propagar a mudança pro backend quando o grupo já está sincronizado (T29,
 * [propagateUpdateIfSynced]) — T24 nunca precisou disso porque criar despesa em grupo sincronizado
 * é lacuna conhecida e documentada à parte (RemoteGroupRepository não cobre despesa avulsa).
 */
class CreateExpenseViewModel(
    private val groupId: String,
    private val expenseId: String? = null,
    private val participantRepository: ParticipantRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val remoteExpenseRepository: RemoteExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExpenseUiState(expenseId = expenseId))
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateExpenseEvent>()
    val events: SharedFlow<CreateExpenseEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val participants = participantRepository.getParticipantsFlow(groupId).first()
            _uiState.update { it.withParticipants(participants) }
            expenseId?.let { loadExpenseForEditing(it) }
        }
    }

    /**
     * T29.1: pré-preenche o formulário com a despesa sendo editada — leitura pontual
     * ([ExpenseRepository.getExpenseById]), não o `Flow` da lista, porque só lemos uma vez, ao
     * abrir a tela (ver KDoc de `ExpenseDao.getExpenseWithSplitsById`). Silenciosamente não faz
     * nada se a despesa já não existir mais (apagada em outra tela enquanto esta abria) — mesma
     * defesa de [com.rateio.app.ui.groupdetail.GroupDetailUiState.NotFound].
     */
    private suspend fun loadExpenseForEditing(expenseId: String) {
        val expense = expenseRepository.getExpenseById(expenseId) ?: return
        _uiState.update { it.withExpenseForEditing(expense) }
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
            try {
                val editingId = expenseId
                if (editingId != null) {
                    updateExistingExpense(state, validation.amountCents, editingId)
                } else {
                    insertNewExpense(state, validation.amountCents)
                }
            } finally {
                // Sem isso, `isSaving` ficava `true` pra sempre — inofensivo enquanto a tela
                // desmontava ao navegar de volta pro grupo logo após o evento abaixo, mas virava
                // bug visível (botão "Salvar despesa" travado desabilitado) assim que o mesmo
                // ViewModel era reaproveitado numa visita seguinte a esta tela (ver key em
                // CreateExpenseRoute). Reseta ANTES de emitir o evento (não depois, num `finally`
                // só em volta do emit também): "salvando" termina quando a despesa é persistida,
                // não quando alguém reage à notificação de navegação — e só assim quem observa
                // [events] já vê `isSaving = false` no mesmo instante em que o evento chega.
                _uiState.update { it.copy(isSaving = false) }
            }
            _events.emit(CreateExpenseEvent.Saved)
        }
    }

    private suspend fun insertNewExpense(state: CreateExpenseUiState, amountCents: Long) {
        val payerId = requireNotNull(state.payerId) { "Nenhum pagador selecionado pro grupo $groupId." }
        expenseRepository.insertExpense(
            state.toExpense(id = UUID.randomUUID().toString(), payerId = payerId, amountCents = amountCents),
        )
    }

    /**
     * T29.1: grava a edição no Room (upsert — [ExpenseRepository.insertExpense] com o mesmo
     * [expenseId] substitui despesa + splits, ver KDoc de `ExpenseDao.insertWithSplits`) e só
     * depois tenta propagar pro backend, nunca o contrário — local-first significa que a edição já
     * vale localmente antes de qualquer tentativa de rede (constitution.md, princípio 1).
     */
    private suspend fun updateExistingExpense(state: CreateExpenseUiState, amountCents: Long, editingExpenseId: String) {
        val payerId = requireNotNull(state.payerId) { "Nenhum pagador selecionado pro grupo $groupId." }
        val expense = state.toExpense(id = editingExpenseId, payerId = payerId, amountCents = amountCents)

        expenseRepository.insertExpense(expense)
        propagateUpdateIfSynced(expense)
    }

    private fun CreateExpenseUiState.toExpense(id: String, payerId: String, amountCents: Long) = Expense(
        id = id,
        groupId = groupId,
        description = description.trim(),
        amountCents = amountCents,
        paidByParticipantId = payerId,
        createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        splits = buildSplits(splitMode, splitRows),
    )

    /**
     * T29: propaga a edição pro backend quando [groupId] já está sincronizado — mesmo padrão de
     * [com.rateio.app.ui.groupdetail.GroupDetailViewModel.onSyncGroupClick] (T19), só que
     * best-effort e silencioso: a edição local já aconteceu na linha acima
     * ([updateExistingExpense]), então uma falha de rede/sessão aqui nunca desfaz nem bloqueia o
     * evento [CreateExpenseEvent.Saved] pra UI (local-first, constitution.md princípio 1 — "mudança
     * local não espera confirmação de servidor", T29-app-editar-excluir-despesa.md). Diferente de
     * "Sincronizar este grupo" (T19), não existe uma ação de "tentar de novo" pra uma única
     * despesa hoje — fica pra quando o app ganhar uma fila de sincronização de verdade.
     */
    private suspend fun propagateUpdateIfSynced(expense: Expense) {
        val group = groupRepository.getGroupById(groupId) ?: return
        val remoteId = group.remoteId?.takeIf { group.isSynced } ?: return
        try {
            remoteExpenseRepository.updateExpense(remoteId, expense)
        } catch (error: GroupSyncException) {
            // Ver KDoc da função: falha de rede/HTTP não desfaz a edição local.
        }
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

/**
 * T29.1: inverso de [buildSplits] — reconstrói [CreateExpenseUiState.splitMode]/[ExpenseSplitRowUiModel]
 * a partir de [Expense.splits] de uma despesa já existente, em vez dos defaults de
 * [withParticipants]. Chamada depois de [withParticipants] no `init` (mesma ordem): as linhas já
 * existem com nome/isYou/percentual padrão semeados, aqui só os campos que a despesa gravada
 * realmente tinha são sobrescritos. Um participante sem split correspondente (entrou no grupo
 * depois que a despesa foi lançada) fica de fora da divisão ao reabrir pra editar — reflete
 * fielmente o que foi salvo, em vez de inventar uma participação que nunca existiu.
 */
private fun CreateExpenseUiState.withExpenseForEditing(expense: Expense): CreateExpenseUiState {
    val splitsByParticipant = expense.splits.associateBy { it.participantId }
    val rows = splitRows.map { row ->
        when (val split = splitsByParticipant[row.participantId]) {
            is ExpenseSplit.Equal -> row.copy(isIncluded = true)
            is ExpenseSplit.Weight -> row.copy(isIncluded = true, percentageInput = split.weight.toString())
            is ExpenseSplit.FixedAmount ->
                row.copy(isIncluded = true, fixedAmountInput = formatCentsAsAmountInput(split.amount.cents))
            null -> row.copy(isIncluded = false, percentageInput = "0", fixedAmountInput = "")
        }
    }
    return copy(
        description = expense.description,
        amountInput = formatCentsAsAmountInput(expense.amountCents),
        payerId = expense.paidByParticipantId,
        date = expense.createdAt.atZone(ZoneId.systemDefault()).toLocalDate(),
        splitMode = expense.splits.toUiSplitMode(),
        splitRows = rows,
    ).withRecalculatedSplit()
}

/** Espelha [com.rateio.app.ui.groupdetail.GroupDetailViewModel]'s `splitTypeLabel()` — deriva do primeiro split. */
private fun List<ExpenseSplit>.toUiSplitMode(): SplitMode = when (firstOrNull()) {
    is ExpenseSplit.Weight -> SplitMode.PERCENTAGE
    is ExpenseSplit.FixedAmount -> SplitMode.FIXED_AMOUNT
    is ExpenseSplit.Equal, null -> SplitMode.EQUAL
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
