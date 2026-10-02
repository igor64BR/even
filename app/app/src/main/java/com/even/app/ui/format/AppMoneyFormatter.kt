package com.even.app.ui.format

import com.even.domain.format.MoneyFormatter

/**
 * Adapts [formatCentsAsBrl] to [MoneyFormatter] — injected into
 * `com.even.data.remote.realtime.SignalRGroupRealtimeGateway` via [com.even.app.di.AppContainer],
 * so `:data` can build the notification text without needing `java.text.NumberFormat`/`Locale`
 * directly. See the KDoc of [MoneyFormatter] for the full rationale.
 */
class AppMoneyFormatter : MoneyFormatter {
    override fun format(amountCents: Long): String = formatCentsAsBrl(amountCents)
}
