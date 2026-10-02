package com.even.domain.format

/**
 * Formats cents into displayable money text ("R$ 45,00") — an abstraction so
 * `com.even.data.remote.realtime.GroupEventNotificationBuilder` doesn't need
 * `java.text.NumberFormat`/`Locale` directly. Formatting is a presentation concern, the same
 * boundary `com.even.app.ui.format.MoneyFormat` already protects for the rest of the app — only
 * the UI edge formats, never the domain or the data layer. Real implementation in `:app`
 * (`AppMoneyFormatter`, reuses `formatCentsAsBrl`), injected into `:data` via `AppContainer` — the
 * same Dependency Inversion pattern [com.even.domain.realtime.GroupRealtimeGateway] uses for the
 * SignalR client.
 */
fun interface MoneyFormatter {
    fun format(amountCents: Long): String
}
