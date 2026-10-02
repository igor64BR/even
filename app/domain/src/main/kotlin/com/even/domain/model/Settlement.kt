package com.even.domain.model

import java.time.Instant

/**
 * A record that [payerId] paid [amount] to [receiverId] to settle (part of) an existing debt.
 * Mirrors `Settlement` (`backend/src/Even.Domain/Settlement.cs`).
 *
 * [groupId] scopes persistence to a group: real persistence via `SettlementRepository`
 * (`:domain`)/`RoomSettlementRepository` (`:data`), the same pattern as [Expense.groupId] — every
 * read is always scoped to a group (the "Settle debts" screen only cares about the settlements of
 * the group it's looking at).
 *
 * [createdAt] is needed to order the "Settlement history" list (the settlement itself carries no
 * other notion of order, and `id` is a UUID unrelated to time).
 */
data class Settlement(
    val id: String,
    val groupId: String,
    val payerId: String,
    val receiverId: String,
    val amount: Money,
    val createdAt: Instant,
)
