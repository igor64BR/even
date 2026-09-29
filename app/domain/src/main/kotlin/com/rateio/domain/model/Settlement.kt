package com.rateio.domain.model

import java.time.Instant

/**
 * A record that [payerId] paid [amount] to [receiverId] to settle (part of) an existing debt
 * (RF31/RF33). Mirrors `Settlement` (`backend/src/Rateio.Domain/Settlement.cs`).
 *
 * [groupId] has existed since T42.1 — real persistence via `SettlementRepository`
 * (`:domain`)/`RoomSettlementRepository` (`:data`), the same pattern as [Expense.groupId]: every
 * read is always scoped to a group (the "Settle debts" screen only cares about the settlements of
 * the group it's looking at). Before T42.1 this type only existed to give `computeBalances`
 * (`algorithm-spec.md`, T33) something to consume — it had no DAO/Room entity of its own.
 *
 * [createdAt] has existed since T37 — needed to order the "Settlement history" list (the
 * settlement itself carries no other notion of order, and `id` is a UUID unrelated to time).
 */
data class Settlement(
    val id: String,
    val groupId: String,
    val payerId: String,
    val receiverId: String,
    val amount: Money,
    val createdAt: Instant,
)
