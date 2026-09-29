package com.tally.data.repository

import com.tally.data.local.auth.TokenStorage
import com.tally.data.remote.groups.GroupsApi
import com.tally.domain.model.Expense
import com.tally.domain.repository.GroupSyncException
import com.tally.domain.repository.RemoteExpenseRepository
import java.io.IOException
import retrofit2.HttpException

/**
 * Implementation of [RemoteExpenseRepository] on top of [GroupsApi] (`PUT`/`DELETE
 * /groups/{id}/expenses/{expenseId}`, T28/T29) + [TokenStorage] (T12.2, reading the access token) —
 * same pattern as [RemoteGroupSyncRepository] (T19.1): no network type leaks into `:domain`, every
 * session/network/HTTP failure becomes a [GroupSyncException] with a message ready for the screen.
 * [Expense.toSyncDto] (`ExpenseSyncMapper.kt`) is the same translation [RemoteGroupSyncRepository]
 * uses in the bulk `POST /groups/sync`.
 */
class RemoteExpenseSyncRepository(
    private val groupsApi: GroupsApi,
    private val tokenStorage: TokenStorage,
) : RemoteExpenseRepository {

    override suspend fun updateExpense(remoteGroupId: String, expense: Expense) {
        val accessToken = requireAccessToken(action = "edit")

        try {
            groupsApi.updateExpense(
                bearerToken = "Bearer $accessToken",
                id = remoteGroupId,
                expenseId = expense.id,
                request = expense.toSyncDto(),
            )
        } catch (error: HttpException) {
            throw GroupSyncException("The Tally server rejected the expense edit.", error)
        } catch (error: IOException) {
            throw GroupSyncException("No connection to the Tally server.", error)
        }
    }

    override suspend fun deleteExpense(remoteGroupId: String, expenseId: String) {
        val accessToken = requireAccessToken(action = "delete")

        try {
            groupsApi.deleteExpense(bearerToken = "Bearer $accessToken", id = remoteGroupId, expenseId = expenseId)
        } catch (error: HttpException) {
            throw GroupSyncException("The Tally server rejected the expense deletion.", error)
        } catch (error: IOException) {
            throw GroupSyncException("No connection to the Tally server.", error)
        }
    }

    private fun requireAccessToken(action: String): String =
        tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("You need to be signed in to $action a synced expense.")
}
