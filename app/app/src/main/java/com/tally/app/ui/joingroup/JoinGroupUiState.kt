package com.tally.app.ui.joingroup

/**
 * State for the "Join group" screen (T22.2, deep link `tally://join/{code}`).
 *
 * There's no "group X" state with a name to show before confirming: the backend (T21) doesn't
 * expose an endpoint to preview the group by its code, only the one to actually join — that's why
 * [Confirming] only carries the [inviteCode] used in the call, never a made-up group name (see
 * `T22-app-entrar-via-link.md`, "don't make one up").
 */
sealed interface JoinGroupUiState {

    /** Session not resolved yet (first emission of [com.tally.domain.repository.AuthRepository.getSessionFlow] hasn't arrived). */
    data object CheckingSession : JoinGroupUiState

    /**
     * Signed out: the action only makes sense authenticated (T21 requires `[Authorize]`). Whoever
     * observes this state is responsible for keeping [inviteCode] and sending the user to the
     * login screen, resuming the flow afterwards — the confirmation screen itself never navigates
     * on its own.
     */
    data class NeedsLogin(val inviteCode: String) : JoinGroupUiState

    /** Signed in, waiting for confirmation — "You've been invited to join a group". */
    data class Confirming(val inviteCode: String) : JoinGroupUiState

    /** "Join" button tapped, call in progress. */
    data object Joining : JoinGroupUiState

    /**
     * `POST /groups/join/{code}` responded successfully. Only [remoteGroupId] — no name,
     * participants or expenses (gap documented in [com.tally.domain.repository.RemoteGroupRepository.joinByCode]),
     * so the UI shows a generic confirmation, not the group's details.
     */
    data class Success(val remoteGroupId: String) : JoinGroupUiState

    /** Network/HTTP failure (includes invalid/expired code) already translated by [com.tally.domain.repository.GroupSyncException]. */
    data class Error(val inviteCode: String, val message: String) : JoinGroupUiState
}
