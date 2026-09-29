package com.tally.domain.model

import java.time.Instant

/**
 * A group of shared expenses. The source of truth is always the device while the group isn't
 * synced (constitution.md, principle 1) — no field here depends on an account or network to
 * exist.
 *
 * [isSynced] mirrors `Synced` from `Group.cs` (backend): `false` for every newly created local
 * group, only becoming `true` once the group is synced with the backend (RF09). `Group.cs`
 * protects that transition with a private setter + a `MarkAsSynced()` method, because there
 * `Group` is an aggregate with its own invariant (at least 1 participant) and no loose setters.
 * This `Group` remains the simple `data class` T7 already modeled (no participant invariant —
 * that lives in `Group.cs`/a future rich-aggregate task on Android, out of scope for T7B), so the
 * same state transition here is `group.copy(isSynced = true)`: idiomatic Kotlin, with no new way
 * opened to mutate the field from outside (the `data class` is already immutable).
 *
 * [remoteId] (T19.2) is the id assigned by the backend on the first sync
 * (`SyncGroupResponse.GroupId`) — `null` while the group only exists on this device. It always
 * travels together with [isSynced]: both become true/populated in the same transition
 * (`group.copy(isSynced = true, remoteId = serverId)`), never one without the other, so the group
 * is never left in an inconsistent state (marked as synced without knowing which remote group it
 * corresponds to, or vice versa).
 */
data class Group(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
)
