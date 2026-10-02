package com.even.data.remote.groups

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Mirrors `POST /groups/sync` (backend: `GroupsController.Sync`,
 * `backend/src/Even.Api/Controllers/GroupsController.cs`) — the first sync of a local
 * group to the backend. Same Retrofit pattern as
 * [com.even.data.remote.auth.AuthApi]: a thin interface, request/response bodies as
 * `@Serializable` DTOs that mirror the C# records field for field (System.Text.Json/ASP.NET Core
 * serializes in camelCase by default, with no enum converter registered — that's why
 * `category`/`splitType` travel as `Int`, the C# enum's ordinal value, not as a string).
 *
 * The endpoint requires a valid JWT (`[Authorize]` on the controller) — `Authorization` is passed
 * as an explicit parameter (not via a global OkHttp interceptor) because today only this endpoint
 * needs it; see the decision recorded in [com.even.data.remote.EvenHttpClientFactory].
 */
interface GroupsApi {
    @POST("groups/sync")
    suspend fun sync(
        @Header("Authorization") bearerToken: String,
        @Body request: SyncGroupRequestDto,
    ): SyncGroupResponseDto

    /**
     * Mirrors `POST /groups/join/{code}` (backend: `GroupsController.JoinByCode`) — joins
     * an existing group via invite code. Response uses the same shape as [sync]
     * (`SyncGroupResponse(GroupId)`), reused here instead of a dedicated DTO because the body is
     * identical.
     */
    @POST("groups/join/{code}")
    suspend fun join(
        @Header("Authorization") bearerToken: String,
        @Path("code") code: String,
    ): SyncGroupResponseDto

    /**
     * Edit expense: mirrors `PUT /groups/{id}/expenses/{expenseId}` on the backend — see the KDoc
     * of [com.even.domain.repository.RemoteExpenseRepository] for a documented pending item. Same
     * body as `POST /groups/{id}/expenses` (`CreateExpense`, same [SyncedExpenseDto]) — only the
     * HTTP verb changes, since it's always the whole expense replacing the previous one, never a
     * partial patch.
     */
    @PUT("groups/{id}/expenses/{expenseId}")
    suspend fun updateExpense(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Path("expenseId") expenseId: String,
        @Body request: SyncedExpenseDto,
    )

    /**
     * Delete expense: mirrors `DELETE /groups/{id}/expenses/{expenseId}` on the backend — same
     * documented pending item as [updateExpense].
     */
    @DELETE("groups/{id}/expenses/{expenseId}")
    suspend fun deleteExpense(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Path("expenseId") expenseId: String,
    )
}

/** Body of `POST /groups/sync` — mirrors the backend's `SyncGroupRequest`. */
@Serializable
data class SyncGroupRequestDto(
    val name: String,
    val category: Int,
    val participants: List<SyncedParticipantDto>,
    val expenses: List<SyncedExpenseDto>,
)

/** Mirrors `SyncedParticipantRequest(Id, Name, IsGuest)`. */
@Serializable
data class SyncedParticipantDto(
    val id: String,
    val name: String,
    val isGuest: Boolean,
)

/**
 * Mirrors `SyncedExpenseRequest`. [date] is `yyyy-MM-dd` (the default format
 * `System.Text.Json` uses for `DateOnly`).
 */
@Serializable
data class SyncedExpenseDto(
    val id: String,
    val description: String,
    val totalAmountCents: Long,
    val payerId: String,
    val date: String,
    val splitType: Int,
    val splits: List<SyncedExpenseSplitDto>,
)

/**
 * Mirrors `SyncedExpenseSplitRequest`. [weight] is only populated for a weighted split,
 * [amountCents] only for a fixed-amount split — mutually exclusive, same as the backend contract.
 */
@Serializable
data class SyncedExpenseSplitDto(
    val participantId: String,
    val weight: Long? = null,
    val amountCents: Long? = null,
)

/** Success response — mirrors the backend's `SyncGroupResponse(GroupId)`. */
@Serializable
data class SyncGroupResponseDto(val groupId: String)
