package com.tally.app.ui.createexpense

import com.tally.app.ui.format.parseAmountInputToCents
import com.tally.domain.model.Money

/**
 * The "Fixed amount" tab: each row holds a freely typed R$ amount (the same `<input
 * type="number" step="0.01">` as the prototype), and saving is only allowed when the sum matches
 * the expense's total amount exactly (`prototype/new-expense.html`, `updateSplitSum`: a 0.01
 * tolerance there because the value is a `parseFloat`; here everything is [Money]/integer cents,
 * so the comparison is exact equality, no floating-point tolerance).
 *
 * Reuses [parseAmountInputToCents] — the same boundary parser as the "Total amount" field
 * ([AmountField]) — instead of reinventing money parsing (money is never a raw
 * `Double`/`Float` outside the input boundary).
 */

/** `null` for an empty/invalid/negative input. */
fun parseFixedAmountInput(input: String): Money? {
    val cents = parseAmountInputToCents(input) ?: return null
    return Money.ofCents(cents)
}

/** Sum of the fixed amounts across all rows; an invalid/empty input counts as zero. */
fun sumFixedAmounts(rows: List<ExpenseSplitRowUiModel>): Money =
    Money.ofCents(rows.sumOf { parseFixedAmountInput(it.fixedAmountInput)?.cents ?: 0L })

/** Gate for "Save expense" in the Fixed amount tab: the sum needs to match the exact total amount. */
fun isFixedAmountSplitComplete(rows: List<ExpenseSplitRowUiModel>, total: Money): Boolean =
    sumFixedAmounts(rows) == total
