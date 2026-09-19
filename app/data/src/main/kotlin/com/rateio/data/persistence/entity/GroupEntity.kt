package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de um grupo. Objeto de mapeamento puro, sem lógica de negócio;
 * conversão para/de `com.rateio.domain.model.Group` mora nos mappers de `:data`, não aqui (ver
 * `com.rateio.data.repository.RoomGroupRepository`).
 *
 * [isSynced] espelha `Group.isSynced` (`:domain`, T7B.1) / `Sincronizado` de `Grupo.cs`.
 * `false` por padrão: todo grupo persistido localmente antes de existir sincronização nasce não
 * sincronizado.
 *
 * [remoteId] espelha `Group.remoteId` (`:domain`, T19.2) — `null` até a primeira sincronização
 * bem-sucedida, coluna nova na v3 do schema (ver `RateioDatabase`).
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
)
