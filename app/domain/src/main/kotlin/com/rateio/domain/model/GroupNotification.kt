package com.rateio.domain.model

import java.time.Instant

/**
 * Notificação local de um evento de grupo sincronizado (T40/T41, RF35/RF36; constitution.md
 * princípio 3 — sistema próprio via Hub, sem push de terceiros) — despesa lançada ou dívida
 * quitada por alguém, recebida em tempo real (SignalR) ou recuperada pelo fallback de pull (T39).
 *
 * [message] já vem pronto pra exibir (fiel a `prototype/notificacoes.html`: "Fulano lançou
 * 'Descrição' — R$X — em 'Nome do grupo'.") — só
 * `com.rateio.data.remote.realtime.GroupEventNotificationBuilder` monta esse texto; `:domain` não
 * precisa saber como.
 *
 * [id] não é um UUID aleatório: é o id do evento no servidor (`despesaId`/`quitacaoId`) prefixado
 * pelo tipo — chave natural que deduplica automaticamente o mesmo evento chegando duas vezes (uma
 * via SignalR em tempo real, outra via T39 no pull de reconexão), sem precisar de lógica extra de
 * "já vi esse evento" (ver `NotificationDao.insert`, `OnConflictStrategy.IGNORE`).
 */
data class GroupNotification(
    val id: String,
    val groupId: String,
    val message: String,
    val occurredAt: Instant,
    val isRead: Boolean = false,
)
