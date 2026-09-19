package com.rateio.app.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EXPENSE_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale("pt", "BR"))

/**
 * "12 mar" — mesma formatação curta de `fmtDate()` em `prototype/app.js`, usada na lista de
 * despesas da tela "Detalhes do grupo" (T42.2). Converte pro fuso do aparelho antes de formatar,
 * já que [instant] é sempre UTC (ver `Expense.createdAt`).
 */
fun formatInstantAsShortDate(instant: Instant): String =
    EXPENSE_DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))
