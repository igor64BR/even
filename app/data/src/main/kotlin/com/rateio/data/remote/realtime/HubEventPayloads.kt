package com.rateio.data.remote.realtime

/**
 * Payload for the SignalR method "ExpenseCreated" — mirrors `ExpenseCreatedEvent`
 * (`Rateio.Application.Notifications`, backend T38.2/T38's `SignalRGroupEventNotifier`, which
 * sends `nameof(GroupEventType.ExpenseCreated)` as the method name with the whole record as the
 * argument). The official Java SignalR client (`com.microsoft.signalr:signalr`) deserializes
 * these payloads with Gson under the hood (confirmed in the artifact's published POM — it depends
 * on `com.google.code.gson:gson`, not Jackson): Gson fills fields via direct reflection, with no
 * getter/setter or specific constructor required, so an idiomatic `data class` with the same field
 * names as the JSON (camelCase — the default `JsonHubProtocol` of ASP.NET Core with no custom
 * converter) already works with no annotations at all. Default values guard against a partial
 * payload ever breaking deserialization.
 */
internal data class ExpenseCreatedPayload(
    val groupId: String = "",
    val expenseId: String = "",
    val description: String = "",
    val totalAmountCents: Long = 0,
    val payerId: String = "",
)

/** Payload for the SignalR method "DebtSettled" — mirrors `DebtSettledEvent`. See [ExpenseCreatedPayload]. */
internal data class DebtSettledPayload(
    val groupId: String = "",
    val settlementId: String = "",
    val fromParticipantId: String = "",
    val toParticipantId: String = "",
    val amountCents: Long = 0,
)
