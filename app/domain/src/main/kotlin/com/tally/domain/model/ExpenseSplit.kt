package com.tally.domain.model

/**
 * A [participantId]'s share of an [Expense]. Mirrors `ExpenseSplit`
 * (`backend/src/Tally.Domain/ExpenseSplit.cs`): the concrete subtype already carries the
 * applicable split rule (Equal/Weight/FixedAmount) — there's no separate `SplitType` enum,
 * because that would leave two places that could diverge (the enum saying one thing, the item
 * being of another subtype). A `sealed class` with nested subtypes is the natural Kotlin
 * equivalent of the `abstract record` + nested `sealed record`s hierarchy `ExpenseSplit.cs` uses
 * in C#.
 *
 * All splits for the same expense must be of the same concrete subtype; validating that
 * consistency is the responsibility of the input boundary (form/DTO), not this type nor the
 * simplification engine — the same decision documented in `ExpenseSplit.cs`.
 */
sealed class ExpenseSplit {
    abstract val participantId: String

    /** Equal split among all participants. */
    data class Equal(override val participantId: String) : ExpenseSplit()

    /** Split proportional to a weight/percentage per participant. */
    data class Weight(override val participantId: String, val weight: Long) : ExpenseSplit()

    /** Split by a fixed amount set per participant. */
    data class FixedAmount(override val participantId: String, val amount: Money) : ExpenseSplit()
}
