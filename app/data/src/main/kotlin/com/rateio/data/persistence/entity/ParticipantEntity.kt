package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de um participante. Schema não exige nenhum campo de
 * autenticação/conta (constitution.md, princípio 1) — [name] é o único dado obrigatório além do
 * vínculo com o grupo.
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
