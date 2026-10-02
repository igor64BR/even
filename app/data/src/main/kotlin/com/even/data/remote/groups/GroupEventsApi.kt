package com.even.data.remote.groups

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mirrors `GET /groups/{id}/events?since=` — the pull fallback that recovers what the realtime
 * connection missed, since there's no third-party push. The
 * backend returns `IReadOnlyList<object>.Cast<object>()` built from concrete
 * `ExpenseCreatedEvent`/`DebtSettledEvent` instances (not the `IGroupEvent` interface, precisely so
 * `System.Text.Json` serializes each type's own fields, not just the interface's) — each list item
 * already comes out with exactly the fields of its respective event (including `type`, the
 * computed `GroupEventType` from the interface).
 *
 * [GroupEventDto] models this heterogeneous list as a single "wide" DTO (all fields from both
 * concrete variants, `null` for whatever doesn't apply to a given item), discriminated by
 * [GroupEventDto.type] — kotlinx.serialization with default field values covers an item that only
 * carries the subset of fields for its own type, without needing a separate `sealed`/polymorphism
 * setup in the client. `type` serializes as `Int` (same enum convention already documented in
 * `RemoteGroupSyncRepository`, since there's no `JsonStringEnumConverter`).
 *
 * **Real gap, confirmed in the backend code**: neither
 * `ExpenseCreatedEvent` nor `DebtSettledEvent` carries a timestamp — `GetGroupEventsUseCase` uses
 * `CreatedAt` only to sort/filter server-side and discards the field before building the response
 * event (`.Select(item => item.Event)`). `com.even.data.remote.realtime.MissedGroupEventsSynchronizer`
 * uses the device's local time at the moment it processes each event as
 * `GroupNotification.occurredAt`, both for this endpoint and for realtime events (same limitation
 * in both cases: no envelope carries the instant the event actually occurred on the server).
 *
 * `since` (`[FromQuery] DateTimeOffset since`, no default value on the backend) is always required
 * — that's why [getEvents] takes `since: String` as non-null; `MissedGroupEventsSynchronizer` sends
 * `Instant.EPOCH` when there's no local notification known yet, never omitting the parameter.
 */
interface GroupEventsApi {
    @GET("groups/{id}/events")
    suspend fun getEvents(
        @Header("Authorization") bearerToken: String,
        @Path("id") groupId: String,
        @Query("since") since: String,
    ): List<GroupEventDto>
}

/** See [GroupEventsApi] — single DTO covering `ExpenseCreatedEvent`/`DebtSettledEvent`, discriminated by [type]. */
@Serializable
data class GroupEventDto(
    val type: Int,
    val groupId: String,
    val expenseId: String? = null,
    val settlementId: String? = null,
    val description: String? = null,
    val totalAmountCents: Long? = null,
    val payerId: String? = null,
    val fromParticipantId: String? = null,
    val toParticipantId: String? = null,
    val amountCents: Long? = null,
)

/** Mirrors `GroupEventType.ExpenseCreated` (ordinal value 0) from the backend. */
const val EVENT_TYPE_EXPENSE_CREATED = 0

/** Mirrors `GroupEventType.DebtSettled` (ordinal value 1) from the backend. */
const val EVENT_TYPE_DEBT_SETTLED = 1
