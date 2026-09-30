package com.tally.data.repository

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.groups.GroupsApi
import com.tally.data.remote.groups.SyncGroupRequestDto
import com.tally.data.remote.groups.SyncedParticipantDto
import com.tally.domain.model.Expense
import com.tally.domain.model.Group
import com.tally.domain.model.Participant
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.RemoteGroupRepository
import java.io.IOException
import retrofit2.HttpException

/**
 * Implementation of [RemoteGroupRepository] on top of [GroupsApi] (`POST /groups/sync`) +
 * [TokenStorage] (reading the access token).
 *
 * Two model gaps, resolved here with the most honest value available
 * (documented, not invented):
 * - [Group] (`:domain`) doesn't model category yet (same gap already flagged by
 *   `CreateGroupViewModel`: the category chosen in the form isn't persisted). Every group
 *   syncs as [GROUP_CATEGORY_OTHER] until a future task adds the real field.
 * - [Participant] (`:domain`) doesn't model a link to an actual account — the project's
 *   local-first stance only requires a name. Every participant
 *   syncs as a guest (`isGuest = true`); there's currently no way for a `Participant` to
 *   correspond to an authenticated user other than the device owner.
 */
class RemoteGroupSyncRepository(
    private val groupsApi: GroupsApi,
    private val tokenStorage: TokenStorage,
) : RemoteGroupRepository {

    override suspend fun syncGroup(group: Group, participants: List<Participant>, expenses: List<Expense>): String {
        val accessToken = tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("You need to be signed in to sync a group.")

        try {
            val response = groupsApi.sync(
                bearerToken = "Bearer $accessToken",
                request = group.toSyncRequest(participants, expenses),
            )
            return response.groupId
        } catch (error: HttpException) {
            throw GroupSyncException("The Tally server rejected the sync.", error)
        } catch (error: IOException) {
            throw GroupSyncException("No connection to the Tally server.", error)
        }
    }

    override suspend fun joinByCode(inviteCode: String): String {
        val accessToken = tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("You need to be signed in to join a group.")

        try {
            val response = groupsApi.join(bearerToken = "Bearer $accessToken", code = inviteCode)
            return response.groupId
        } catch (error: HttpException) {
            throw GroupSyncException("Couldn't join that group — check the code.", error)
        } catch (error: IOException) {
            throw GroupSyncException("No connection to the Tally server.", error)
        }
    }
}

private fun Group.toSyncRequest(participants: List<Participant>, expenses: List<Expense>) =
    SyncGroupRequestDto(
        name = name,
        category = GROUP_CATEGORY_OTHER,
        participants = participants.map(Participant::toSyncDto),
        expenses = expenses.map(Expense::toSyncDto),
    )

private fun Participant.toSyncDto() = SyncedParticipantDto(
    id = id,
    name = name,
    isGuest = true,
)

// Expense.toSyncDto()/split translation live in ExpenseSyncMapper.kt (same package,
// `internal` — shared with RemoteExpenseSyncRepository, to avoid duplicating the same
// Expense -> SyncedExpenseDto translation in both places).

// Mirrors the ordinal value of GroupCategory.Other (C#) — the backend doesn't register a
// JsonStringEnumConverter, so System.Text.Json serializes/deserializes the enum as an Int.
private const val GROUP_CATEGORY_OTHER = 4
