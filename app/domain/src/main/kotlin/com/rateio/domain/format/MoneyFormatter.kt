package com.rateio.domain.format

/**
 * Formata centavos pra texto monetário exibível ("R$ 45,00") — abstração pra
 * `com.rateio.data.remote.realtime.GroupEventNotificationBuilder` (T40/T41) não precisar de
 * `java.text.NumberFormat`/`Locale` diretamente. Formatação é preocupação de apresentação, mesma
 * fronteira que `com.rateio.app.ui.format.MoneyFormat` já protege pro resto do app (algorithm-spec.md,
 * "Dinheiro: representação e arredondamento" — só a borda de UI formata, nunca o domínio ou a
 * camada de dados). Implementação real em `:app` (`AppMoneyFormatter`, reaproveita
 * `formatCentsAsBrl`), injetada em `:data` via `AppContainer` — o mesmo padrão de Dependency
 * Inversion que [com.rateio.domain.realtime.GroupRealtimeGateway] usa pro cliente SignalR.
 */
fun interface MoneyFormatter {
    fun format(amountCents: Long): String
}
