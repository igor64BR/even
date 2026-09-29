package com.rateio.app.ui.createexpense

/**
 * The "Percentage" tab (T26.1): each row holds a freely typed integer `%` (the same `<input
 * type="number" min="0" max="100">` as the prototype — the min/max attributes don't block
 * anything in JavaScript, only the final sum is validated), and saving is only allowed when the
 * sum matches exactly 100 (`prototype/new-expense.html`, `updateSplitSum`/the `salvar-btn`
 * handler: uses a 0.5 tolerance there because percentages can have decimal places; here the
 * percentage is always an integer `Long` —
 * [com.rateio.domain.model.ExpenseSplit.Weight.weight] is also `Long` — so the sum is compared
 * with exact equality, no tolerance).
 *
 * Small, pure functions (Object Calisthenics), independently testable — no sum or parsing lives in
 * the percentage Composables.
 */

/** `null` for an empty/invalid/negative input — the same convention as [parseAmountInputToCents]. */
fun parsePercentageInput(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.toLongOrNull()?.takeIf { it >= 0 }
}

/** Default percentage when loading the tab: `Math.round(100 / n)`, same as the prototype. Doesn't
 * add up to 100% on its own when `n` doesn't divide 100 exactly — the user needs to adjust, on
 * purpose, the same behavior as there. */
fun defaultPercentage(participantCount: Int): Long {
    if (participantCount <= 0) return 0L
    return Math.round(100.0 / participantCount)
}

/** Sum of the percentages across all rows; an invalid/empty input counts as 0. */
fun sumPercentages(rows: List<ExpenseSplitRowUiModel>): Long =
    rows.sumOf { parsePercentageInput(it.percentageInput) ?: 0L }

/** Gate for "Save expense" in the Percentage tab: the sum needs to add up to exactly 100%. */
fun isPercentageSplitComplete(rows: List<ExpenseSplitRowUiModel>): Boolean =
    sumPercentages(rows) == FULL_PERCENTAGE

private const val FULL_PERCENTAGE = 100L
