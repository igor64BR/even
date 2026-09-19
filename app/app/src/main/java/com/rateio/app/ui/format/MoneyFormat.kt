package com.rateio.app.ui.format

import java.text.NumberFormat
import java.util.Locale

/**
 * "R$ 45,00" — mesma formatação de `fmtMoney()` em `prototype/app.js` (pt-BR, duas casas
 * decimais), a partir do valor em centavos (convenção de dinheiro do domínio, ver
 * `Expense.amountCents`).
 */
fun formatCentsAsBrl(amountCents: Long): String {
    val reais = amountCents / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(reais)
}
