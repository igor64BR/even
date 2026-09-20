package com.rateio.app.ui.format

import com.rateio.domain.format.MoneyFormatter

/**
 * Adapta [formatCentsAsBrl] pra [MoneyFormatter] (T40/T41) — injetada em
 * `com.rateio.data.remote.realtime.SignalRGroupRealtimeGateway` via [com.rateio.app.di.AppContainer],
 * pra `:data` montar o texto da notificação sem precisar de `java.text.NumberFormat`/`Locale`
 * diretamente. Ver KDoc de [MoneyFormatter] pro racional completo.
 */
class AppMoneyFormatter : MoneyFormatter {
    override fun format(amountCents: Long): String = formatCentsAsBrl(amountCents)
}
