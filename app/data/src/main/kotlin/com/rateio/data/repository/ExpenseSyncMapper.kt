package com.rateio.data.repository

import com.rateio.data.remote.groups.SyncedExpenseDto
import com.rateio.data.remote.groups.SyncedExpenseSplitDto
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Translates [Expense]/[ExpenseSplit] (`:domain`) into the expense DTO the backend expects
 * (`SyncedExpenseRequest`, T18/T23.1) — extracted from [RemoteGroupSyncRepository] (T19.1, where
 * it originated) to be shared with [RemoteExpenseSyncRepository] (T29): both classes send the same
 * expense shape to the backend (bulk in [RemoteGroupSyncRepository.syncGroup], a single one in
 * [RemoteExpenseSyncRepository.updateExpense]), so the Expense -> DTO translation lives in one
 * place only.
 */
internal fun Expense.toSyncDto() = SyncedExpenseDto(
    id = id,
    description = description,
    totalAmountCents = amountCents,
    payerId = paidByParticipantId,
    date = dateIsoFormatter.format(createdAt.atZone(ZoneOffset.UTC).toLocalDate()),
    splitType = splitTypeOrdinalFor(splits),
    splits = splits.map(ExpenseSplit::toSyncDto),
)

private fun ExpenseSplit.toSyncDto(): SyncedExpenseSplitDto = when (this) {
    is ExpenseSplit.Equal -> SyncedExpenseSplitDto(participantId = participantId)
    is ExpenseSplit.Weight -> SyncedExpenseSplitDto(participantId = participantId, weight = weight)
    is ExpenseSplit.FixedAmount ->
        SyncedExpenseSplitDto(participantId = participantId, amountCents = amount.cents)
}

/**
 * `SplitType` is unique per expense, not per split (same rule as the backend's
 * `SyncedExpenseRequest`) — derived from the first split. An empty list (an expense with no split
 * applied) falls back to the safe default [SPLIT_TYPE_EQUAL].
 */
private fun splitTypeOrdinalFor(splits: List<ExpenseSplit>): Int = when (splits.firstOrNull()) {
    is ExpenseSplit.Weight -> SPLIT_TYPE_WEIGHTED
    is ExpenseSplit.FixedAmount -> SPLIT_TYPE_FIXED_AMOUNT
    is ExpenseSplit.Equal, null -> SPLIT_TYPE_EQUAL
}

private val dateIsoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// Mirror the ordinal values of SplitTypeRequest (C#) — the backend doesn't register a
// JsonStringEnumConverter, so System.Text.Json serializes/deserializes the enum as an Int.
private const val SPLIT_TYPE_EQUAL = 0
private const val SPLIT_TYPE_WEIGHTED = 1
private const val SPLIT_TYPE_FIXED_AMOUNT = 2
