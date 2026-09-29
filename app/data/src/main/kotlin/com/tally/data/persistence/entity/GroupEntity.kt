package com.tally.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistence mapping for a group. A pure mapping object, no business logic; conversion to/from
 * `com.tally.domain.model.Group` lives in `:data`'s mappers, not here (see
 * `com.tally.data.repository.RoomGroupRepository`).
 *
 * [isSynced] mirrors `Group.isSynced` (`:domain`, T7B.1) / `Synced` from `Group.cs`. `false` by
 * default: every group persisted locally before any sync exists starts out unsynced.
 *
 * [remoteId] mirrors `Group.remoteId` (`:domain`, T19.2) — `null` until the first successful sync,
 * a new column in schema v3 (see `TallyDatabase`).
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
)
