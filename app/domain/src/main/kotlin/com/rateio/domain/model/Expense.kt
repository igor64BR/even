package com.rateio.domain.model

import java.time.Instant

/**
 * Despesa lançada em um grupo. Quem pagou, quanto, e como o valor se divide entre os
 * participantes (T7B, RF17-RF19) — o motor de simplificação de dívidas em si (RF25-RF28, que
 * consome [splits] pra calcular saldo) é T33, escopo futuro.
 *
 * [amountCents] guarda o valor total em centavos (Long), nunca decimal — mesma convenção do
 * `Dinheiro` do motor em Rateio.Domain (backend, T31), para as duas portas do algoritmo (C# já
 * implementado, Kotlin ainda por vir) tratarem dinheiro de forma idêntica. Fica como `Long` cru
 * (em vez de [Money]) porque é o valor total já persistido desde T7/T8 e nenhuma regra nova de
 * T7B precisa dele wrapped — só [ExpenseSplit.FixedAmount] introduz uma parte monetária nova, e
 * essa sim usa [Money], espelhando `Despesa.cs` (que também mantém `ValorTotal` como `Dinheiro`
 * mas cada `ParticipacaoDespesa.PorValorFixo` com seu próprio `Dinheiro`).
 *
 * [splits] espelha `Despesa.Participacoes` de `Despesa.cs` — a lista de [ExpenseSplit], uma por
 * participante da divisão. Vazia por padrão pra não quebrar nenhum código existente (T8) que
 * ainda constrói `Expense` sem se importar com divisão.
 */
data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amountCents: Long,
    val paidByParticipantId: String,
    val createdAt: Instant,
    val splits: List<ExpenseSplit> = emptyList(),
)
