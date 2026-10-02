package com.even.app.ui.createexpense

import com.even.domain.model.Money

/**
 * Splits [total] into equal shares among [participantIds], closing out the remainder using the
 * same largest-remainder rule implemented in `GreedyDebtSimplificationEngine.splitEqually`: sorts
 * by `participantId` ascending, `base = total / n`, `remainder = total % n`, and the first
 * `remainder` participants (in that order) get one extra cent — never naive `total.cents / n`,
 * which loses a cent (e.g. R$10.00 ÷ 3 = 333+333+333 = 999 ≠ 1000).
 *
 * Reimplemented here — a small, pure, independently testable function (Object Calisthenics: no
 * inline calculation in the Composable) — instead of a direct call to the engine because
 * `splitEqually` is private to [com.even.domain.engine.GreedyDebtSimplificationEngine] and this
 * module is restricted to `app/app/`; the rule is the same, line by line, only the participant
 * is responsible for computing the form's live preview, not the group's balance.
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
