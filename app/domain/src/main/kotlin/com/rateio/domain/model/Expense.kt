package com.rateio.domain.model

import java.time.Instant

/**
 * An expense logged in a group. Who paid, how much, and how the amount splits among the
 * participants (T7B, RF17-RF19) — the debt-simplification engine itself (RF25-RF28, which
 * consumes [splits] to compute balance) is T33, future scope.
 *
 * [amountCents] holds the total amount in cents (Long), never decimal — the same convention as
 * `Money` in the Rateio.Domain engine (backend, T31), so both ports of the algorithm (C# already
 * implemented, Kotlin still to come) treat money identically. It stays a raw `Long` (instead of
 * [Money]) because it's the total amount already persisted since T7/T8 and no new T7B rule needs
 * it wrapped — only [ExpenseSplit.FixedAmount] introduces a new monetary part, and that one does
 * use [Money], mirroring `Expense.cs` (which also keeps `TotalAmount` as `Money` but each
 * `ExpenseSplit.FixedAmount` with its own `Money`).
 *
 * [splits] mirrors `Expense.Splits` from `Expense.cs` — the list of [ExpenseSplit], one per
 * participant in the split. Empty by default so it doesn't break any existing code (T8) that
 * still builds an `Expense` without caring about the split.
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
