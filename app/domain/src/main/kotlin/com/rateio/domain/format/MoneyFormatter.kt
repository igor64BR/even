package com.rateio.domain.format

/**
 * Formats cents into displayable money text ("R$ 45,00") — an abstraction so
 * `com.rateio.data.remote.realtime.GroupEventNotificationBuilder` (T40/T41) doesn't need
 * `java.text.NumberFormat`/`Locale` directly. Formatting is a presentation concern, the same
 * boundary `com.rateio.app.ui.format.MoneyFormat` already protects for the rest of the app
 * (algorithm-spec.md, "Money: representation and rounding" — only the UI edge formats, never the
 * domain or the data layer). Real implementation in `:app` (`AppMoneyFormatter`, reuses
 * `formatCentsAsBrl`), injected into `:data` via `AppContainer` — the same Dependency Inversion
 * pattern [com.rateio.domain.realtime.GroupRealtimeGateway] uses for the SignalR client.
 */
fun interface MoneyFormatter {
    fun format(amountCents: Long): String
}
