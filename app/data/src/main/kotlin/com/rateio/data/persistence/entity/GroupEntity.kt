package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de um grupo. Esqueleto mínimo (T7) — objeto de mapeamento puro,
 * sem lógica de negócio; conversão para/de `com.rateio.domain.model.Group` mora nos mappers de
 * `:data`, não aqui (ver `com.rateio.data.repository.RoomGroupRepository`).
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
)
