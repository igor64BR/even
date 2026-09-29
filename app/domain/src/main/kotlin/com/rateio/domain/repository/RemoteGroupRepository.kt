package com.rateio.domain.repository

import com.rateio.domain.model.Expense
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant

/**
 * Contract for syncing a local group to the backend (T19, RF09: `POST /groups/sync`, T18).
 * `:domain` declares it, `:data` implements it on top of Retrofit — no network type
 * (Retrofit/OkHttp) leaks into this interface (Dependency Inversion, same convention as
 * [AuthRepository]).
 *
 * The action only makes sense for an authenticated user (there's no "sync without being signed
 * in" — see T19-app-sincronizar-grupo.md); the caller guarantees that in the UI (only showing the
 * action with an active session). The implementation still validates again before calling the
 * network, to never rely solely on the UI catching it.
 */
interface RemoteGroupRepository {

    /**
     * Sends the group's full local state (group + participants + expenses already logged) to the
     * backend for the first time. Returns the remote id assigned by the server
     * (`SyncGroupResponse.GroupId`) — the caller is responsible for persisting that value into
     * [Group.remoteId] and marking [Group.isSynced]; this function doesn't change any local state,
     * it only talks to the backend.
     *
     * @throws GroupSyncException if there's no authenticated session, or if the network/HTTP call
     * fails — never lets a Retrofit/OkHttp exception leak out to the caller.
     */
    suspend fun syncGroup(group: Group, participants: List<Participant>, expenses: List<Expense>): String

    /**
     * Joins an existing group via an invite code (T21, RF07: `POST /groups/join/{code}`, any
     * authenticated user can join — the valid code IS the authorization, there's no owner check).
     * Returns the group's remote id (`groupId`), in the same response shape as [syncGroup].
     *
     * Known GAP (documented in `specs/001-mvp-expense-splitting/tasks/T22-app-entrar-via-link.md`,
     * not invented here): T21 doesn't return the group's name, participants or expenses, and
     * there's currently no full "get group by id" endpoint to download that data after joining.
     * The caller only gets the remote id [String] back and has no way to build a faithful local
     * [com.rateio.domain.model.Group] (real name, participants, expenses) without inventing data —
     * that's why the confirmation screen (T22.2) shows a generic success and doesn't insert a
     * "fake" group into the local list. This is left for when the backend gets that endpoint.
     *
     * @throws GroupSyncException if there's no authenticated session, or if the network/HTTP call
     * fails — including an invalid/expired invite code (`InvalidInviteCodeException` on the
     * backend becomes a generic HTTP error, T21 doesn't distinguish it from any other failure in a
     * structured body, so the message here stays generic too, the same convention as [syncGroup]).
     */
    suspend fun joinByCode(inviteCode: String): String
}

/**
 * A sync failure already translated into a presentable message — same convention as
 * [AuthenticationFailedException]: the caller of [RemoteGroupRepository.syncGroup] never sees a
 * network type, only the message already ready for the screen (and the original error in [cause],
 * for logging).
 */
class GroupSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)
