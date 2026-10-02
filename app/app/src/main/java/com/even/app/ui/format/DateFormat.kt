package com.even.app.ui.format

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EXPENSE_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/**
 * "12 Mar" — the same short formatting as `fmtDate()` in `prototype/app.js`, used in the expense
 * list of the "Group details" screen. Converts to the device's time zone before
 * formatting, since [instant] is always UTC (see `Expense.createdAt`).
 */
fun formatInstantAsShortDate(instant: Instant): String =
    EXPENSE_DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))

/**
 * "today" / "yesterday" / "3 days ago" / "2 months ago" — the same scale as `fmtRelative()` in
 * `prototype/app.js`, used in the notification list. [now] only exists to make the test
 * deterministic (`DateFormatTest`); every real caller uses the default.
 */
fun formatInstantAsRelative(instant: Instant, now: Instant = Instant.now()): String {
    val days = Duration.between(instant, now).toDays()
    return when {
        days <= 0 -> "today"
        days == 1L -> "yesterday"
        days < 30 -> "$days days ago"
        else -> {
            val months = days / 30
            "$months ${if (months == 1L) "month" else "months"} ago"
        }
    }
}
