package com.tally.app.ui.createexpense

import com.tally.domain.model.Money

/**
 * Splits [total] into equal shares among [participantIds], closing out the remainder using the
 * same largest-remainder rule described in `algorithm-spec.md` ("Closing splits that don't divide
 * evenly") and implemented in `GreedyDebtSimplificationEngine.splitEqually` (T33): sorts by
 * `participantId` ascending, `base = total / n`, `remainder = total % n`, and the first
 * `remainder` participants (in that order) get one extra cent — never naive `total.cents / n`,
 * which loses a cent (e.g. R$10.00 ÷ 3 = 333+333+333 = 999 ≠ 1000).
 *
 * Reimplemented here — a small, pure, independently testable function (Object Calisthenics: no
 * inline calculation in the Composable) — instead of a direct call to the engine because
 * `splitEqually` is private to [com.tally.domain.engine.GreedyDebtSimplificationEngine] and this
 * task (T24) is restricted to `app/app/`; the rule is the same, line by line, only the participant
 * is responsible for computing the form's live preview, not the group's balance (that's T25/T33).
 */
fun calculateEqualSplit(total: Money, participantIds: List<String>): Map<String, Money> {
    if (participantIds.isEmpty()) return emptyMap()

    val sortedIds = participantIds.sorted()
    val count = sortedIds.size
    val base = total.cents / count
    val remainder = total.cents % count

    return sortedIds.mapIndexed { index, participantId ->
        val extraCent = if (index < remainder) 1 else 0
        participantId to Money.ofCents(base + extraCent)
    }.toMap()
}
