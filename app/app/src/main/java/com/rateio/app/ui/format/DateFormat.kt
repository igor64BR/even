package com.rateio.app.ui.format

import java.time.Duration
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

/**
 * "hoje" / "ontem" / "há 3 dias" / "há 2 meses" — mesma escala de `fmtRelative()` em
 * `prototype/app.js`, usada na lista de notificações (T41.1). [now] só existe pra deixar o teste
 * determinístico (`DateFormatTest`); todo chamador real usa o default.
 */
fun formatInstantAsRelative(instant: Instant, now: Instant = Instant.now()): String {
    val days = Duration.between(instant, now).toDays()
    return when {
        days <= 0 -> "hoje"
        days == 1L -> "ontem"
        days < 30 -> "há $days dias"
        else -> {
            val months = days / 30
            "há $months ${if (months == 1L) "mês" else "meses"}"
        }
    }
}
