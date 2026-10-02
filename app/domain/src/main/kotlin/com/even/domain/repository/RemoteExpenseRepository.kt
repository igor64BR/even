package com.even.domain.repository

import com.even.domain.model.Expense

/**
 * Contract for propagating an expense edit/delete to the backend, for a group that's already
 * synced. `:domain` declares it, `:data` implements it on top of Retrofit — no network type
 * (Retrofit/OkHttp) leaks into this interface, the same Dependency Inversion as
 * [RemoteGroupRepository].
 *
 * A separate interface from [RemoteGroupRepository] (Interface Segregation): syncing a whole
 * group (bulk, first sync) and editing/deleting a single already-synced expense are operations
 * with very different shapes — [RemoteGroupRepository.syncGroup] takes the whole group
 * (participants + expenses), here just the expense that changed.
 *
 * The action only makes sense for a [com.even.domain.model.Group] with `isSynced = true` — the
 * caller guarantees that before invoking it (same convention as
 * [RemoteGroupRepository.syncGroup]: local-first, the local change already happened before this
 * call, and a failure here never undoes the local change).
 */
interface RemoteExpenseRepository {

    /**
     * Sends the current version of [expense] (already persisted locally) to the backend,
     * replacing the expense with id [Expense.id] inside the remote group [remoteGroupId].
     *
     * @throws GroupSyncException if there's no authenticated session, or if the network/HTTP call
     * fails — never lets a Retrofit/OkHttp exception leak out to the caller.
     */
    suspend fun updateExpense(remoteGroupId: String, expense: Expense)

    /**
     * Removes the expense [expenseId] from the remote group [remoteGroupId].
     *
     * @throws GroupSyncException if there's no authenticated session, or if the network/HTTP call
     * fails — never lets a Retrofit/OkHttp exception leak out to the caller.
     */
    suspend fun deleteExpense(remoteGroupId: String, expenseId: String)
}
