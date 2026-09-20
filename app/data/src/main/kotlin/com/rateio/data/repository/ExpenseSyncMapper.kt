package com.rateio.data.repository

import com.rateio.data.remote.groups.DespesaSincronizadaDto
import com.rateio.data.remote.groups.ParticipacaoSincronizadaDto
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Traduz [Expense]/[ExpenseSplit] (`:domain`) pro DTO de despesa que o backend espera
 * (`DespesaSincronizadaRequest`, T18/T23.1) — extraído de [RemoteGroupSyncRepository] (T19.1, onde
 * nasceu) pra ser compartilhado com [RemoteExpenseSyncRepository] (T29): as duas classes enviam a
 * mesma forma de despesa pro backend (bulk em [RemoteGroupSyncRepository.syncGroup], uma só em
 * [RemoteExpenseSyncRepository.updateExpense]), então a tradução Expense -> DTO mora num lugar só.
 */
internal fun Expense.toSyncDto() = DespesaSincronizadaDto(
    id = id,
    descricao = description,
    valorTotalCentavos = amountCents,
    pagadorId = paidByParticipantId,
    data = dataIsoFormatter.format(createdAt.atZone(ZoneOffset.UTC).toLocalDate()),
    tipoDivisao = tipoDivisaoOrdinalFor(splits),
    participacoes = splits.map(ExpenseSplit::toSyncDto),
)

private fun ExpenseSplit.toSyncDto(): ParticipacaoSincronizadaDto = when (this) {
    is ExpenseSplit.Equal -> ParticipacaoSincronizadaDto(participanteId = participantId)
    is ExpenseSplit.Weight -> ParticipacaoSincronizadaDto(participanteId = participantId, peso = weight)
    is ExpenseSplit.FixedAmount ->
        ParticipacaoSincronizadaDto(participanteId = participantId, valorCentavos = amount.cents)
}

/**
 * `TipoDivisao` é único por despesa, não por participação (mesma regra do
 * `DespesaSincronizadaRequest` do backend) — deriva do primeiro split. Lista vazia (despesa sem
 * divisão lançada) cai no default seguro [TIPO_DIVISAO_POR_IGUAL].
 */
private fun tipoDivisaoOrdinalFor(splits: List<ExpenseSplit>): Int = when (splits.firstOrNull()) {
    is ExpenseSplit.Weight -> TIPO_DIVISAO_POR_PESO
    is ExpenseSplit.FixedAmount -> TIPO_DIVISAO_POR_VALOR_FIXO
    is ExpenseSplit.Equal, null -> TIPO_DIVISAO_POR_IGUAL
}

private val dataIsoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// Espelham os valores ordinais de TipoDivisaoRequest (C#) — o backend não registra
// JsonStringEnumConverter, então System.Text.Json serializa/desserializa enum como Int.
private const val TIPO_DIVISAO_POR_IGUAL = 0
private const val TIPO_DIVISAO_POR_PESO = 1
private const val TIPO_DIVISAO_POR_VALOR_FIXO = 2
