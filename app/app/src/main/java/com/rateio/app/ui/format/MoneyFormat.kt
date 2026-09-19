package com.rateio.app.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
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

/**
 * Parseia o texto digitado no campo "Valor total" (`#valor` do protótipo) pra centavos. Aceita
 * vírgula ou ponto como separador decimal (o teclado numérico do Android não força um só). `null`
 * pra entrada vazia ou inválida — quem chama trata como "sem valor ainda" (T24.2/T24.4), não como
 * zero.
 *
 * `BigDecimal` aqui é a borda de entrada do usuário, não o motor — exatamente a exceção que
 * `algorithm-spec.md` (seção "Dinheiro: representação e arredondamento") documenta: parsing de
 * entrada e formatação de saída podem usar `BigDecimal`/`double`, só o algoritmo em si não pode.
 * Resultado sempre em centavos (`Long`), a única representação monetária que atravessa a borda
 * pra dentro do domínio.
 */
fun parseAmountInputToCents(input: String): Long? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isEmpty()) return null

    val amount = normalized.toBigDecimalOrNull() ?: return null
    if (amount.signum() < 0) return null

    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
}
