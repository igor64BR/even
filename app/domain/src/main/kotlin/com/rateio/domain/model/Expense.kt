package com.rateio.domain.model

import java.time.Instant

/**
 * Despesa lançada em um grupo. Esqueleto mínimo (T7) — quem pagou e quanto; a divisão entre
 * participantes (igual/peso/valor fixo, RF17-RF19) e o motor de simplificação de dívidas
 * (RF25-RF28) ficam para uma task futura de modelagem rica, análoga ao T15/T23 do backend
 * (`backend/src/Rateio.Infrastructure/Persistence/Entities/DespesaEntity.cs` segue o mesmo
 * corte de escopo do lado .NET).
 *
 * [amountCents] guarda o valor em centavos (Long), nunca decimal — mesma convenção do
 * `Dinheiro` do motor em Rateio.Domain (backend, T31), para as duas portas do algoritmo
 * (C# já implementado, Kotlin ainda por vir) tratarem dinheiro de forma idêntica.
 */
data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amountCents: Long,
    val paidByParticipantId: String,
    val createdAt: Instant,
)
