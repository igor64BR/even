package com.rateio.domain.repository

import com.rateio.domain.model.GroupNotification
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistência de notificações locais (T40/T41, RF35/RF36). `:domain` declara,
 * `:data` implementa com Room (Dependency Inversion) — mesmo padrão de [SettlementRepository]:
 * nenhum tipo do Room vaza para esta interface.
 */
interface NotificationRepository {
    fun getNotificationsFlow(): Flow<List<GroupNotification>>

    /** Contagem de não lidas — fonte do badge da aba "Avisos" (T41.2, `RateioBottomBar`). */
    fun getUnreadCountFlow(): Flow<Int>

    /** Idempotente por [GroupNotification.id] — ver KDoc de [GroupNotification.id]. */
    suspend fun insert(notification: GroupNotification)

    /** Tela "Notificações" (T41.1) marca tudo como lido ao ser fechada — fiel ao protótipo. */
    suspend fun markAllAsRead()

    /**
     * Instante da notificação mais recente já persistida — usado como `desde` na chamada de
     * fallback de pull (T39, ver `com.rateio.domain.realtime.GroupRealtimeGateway`). `null` se
     * nenhuma notificação foi gravada ainda (o pull busca o histórico inteiro disponível).
     */
    suspend fun getLastEventTimestamp(): Instant?
}
