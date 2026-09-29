package com.tally.app.ui.format

import com.tally.domain.format.MoneyFormatter

/**
 * Adapts [formatCentsAsBrl] to [MoneyFormatter] (T40/T41) — injected into
 * `com.tally.data.remote.realtime.SignalRGroupRealtimeGateway` via [com.tally.app.di.AppContainer],
 * so `:data` can build the notification text without needing `java.text.NumberFormat`/`Locale`
 * directly. See the KDoc of [MoneyFormatter] for the full rationale.
 */
class AppMoneyFormatter : MoneyFormatter {
    override fun format(amountCents: Long): String = formatCentsAsBrl(amountCents)
}
