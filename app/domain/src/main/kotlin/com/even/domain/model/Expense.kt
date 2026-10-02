package com.even.domain.model

import java.time.Instant

/**
 * An expense logged in a group. Who paid, how much, and how the amount splits among the
 * participants — the debt-simplification engine consumes [splits] to compute each participant's
 * balance.
 *
 * [amountCents] holds the total amount in cents (Long), never decimal — the same convention as
 * `Money` in the Even.Domain engine (backend), so both ports of the algorithm treat money
 * identically. It stays a raw `Long` instead of [Money] here; only [ExpenseSplit.FixedAmount]
 * introduces a new monetary part, and that one does use [Money], mirroring `Expense.cs` (which
 * also keeps `TotalAmount` as `Money` but each `ExpenseSplit.FixedAmount` with its own `Money`).
 *
 * [splits] mirrors `Expense.Splits` from `Expense.cs` — the list of [ExpenseSplit], one per
 * participant in the split. Empty by default so it doesn't break any existing code that still
 * builds an `Expense` without caring about the split.
 */
data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amountCents: Long,
    val paidByParticipantId: String,
    val createdAt: Instant,
    val splits: List<ExpenseSplit> = emptyList(),
)
