package com.tally.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence mapping for a participant. The schema requires no authentication/account field
 * (constitution.md, principle 1) — [name] is the only required piece of data besides the link to
 * the group.
 */
@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val isYou: Boolean = false,
)
