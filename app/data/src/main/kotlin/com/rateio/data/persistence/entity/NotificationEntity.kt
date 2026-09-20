package com.rateio.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mapeamento de persistência de `com.rateio.domain.model.GroupNotification` (T40.1/T41.1). [id] é
 * o id do evento no servidor prefixado pelo tipo ("despesa:"/"quitacao:", ver
 * `com.rateio.data.remote.realtime.GroupEventNotificationBuilder`) — chave natural que deduplica
 * automaticamente o mesmo evento chegando duas vezes (tempo real + pull de reconexão, T40.2) via
 * `OnConflictStrategy.IGNORE` em vez de um UUID aleatório.
 *
 * [occurredAtEpochMillis] é o horário local do aparelho no momento em que o evento foi processado,
 * não um timestamp de servidor — nem o payload em tempo real (`EventoDespesaCriada`/
 * `EventoDividaQuitada`, backend T38) nem a resposta do fallback de pull (T39) carregam um
 * timestamp de quando o evento aconteceu (lacuna documentada em
 * `com.rateio.data.remote.groups.GroupEventsApi`). Schema v5 (T40.1) — sem migration explícita,
 * mesmo racional das versões anteriores (`RateioDatabase`): ainda não existe build distribuído.
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId"), Index("occurredAtEpochMillis")],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val message: String,
    val occurredAtEpochMillis: Long,
    val isRead: Boolean,
)
