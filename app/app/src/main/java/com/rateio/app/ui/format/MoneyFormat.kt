package com.rateio.app.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * "R$ 45,00" — the same formatting as `fmtMoney()` in `prototype/app.js` (pt-BR, two decimal
 * places), from the amount in cents (the domain's money convention, see `Expense.amountCents`).
 */
fun formatCentsAsBrl(amountCents: Long): String {
    val reais = amountCents / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(reais)
}

/**
 * Parses the text typed into the "Total amount" field (the prototype's `#valor`) into cents.
 * Accepts comma or dot as the decimal separator (Android's numeric keyboard doesn't force just
 * one). `null` for an empty or invalid input — the caller treats that as "no amount yet"
 * (T24.2/T24.4), not as zero.
 *
 * `BigDecimal` here is the user input boundary, not the engine — exactly the exception
 * `algorithm-spec.md` (the "Money: representation and rounding" section) documents: input parsing
 * and output formatting can use `BigDecimal`/`double`, only the algorithm itself can't. The result
 * is always in cents (`Long`), the only monetary representation that crosses the boundary into the
 * domain.
 */
fun parseAmountInputToCents(input: String): Long? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isEmpty()) return null

    val amount = normalized.toBigDecimalOrNull() ?: return null
    if (amount.signum() < 0) return null

    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
}

/**
 * The inverse of [parseAmountInputToCents]: cents -> editable text ("1000" -> "10,00"), with no
 * currency prefix (unlike [formatCentsAsBrl], which is presentation-only — the amount fields
 * already have their own "R$" prefix, see [com.rateio.app.ui.createexpense.AmountField]). Used
 * only to pre-fill amount fields in edit mode (T29): built symmetric to the parser on purpose, for
 * an exact round-trip (load -> edit without touching -> save preserves the same cents).
 */
fun formatCentsAsAmountInput(amountCents: Long): String =
    BigDecimal(amountCents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
