package com.rateio.domain.model

/**
 * Registro de que [payerId] pagou [amount] a [receiverId] para quitar (parte de) uma dívida
 * existente (RF31/RF33). Espelha `Quitacao` (`backend/src/Rateio.Domain/Quitacao.cs`).
 *
 * Sem persistência própria ainda — não há `SettlementRepository`/DAO/entidade Room em `:data`
 * nesta task (T7B pede só o tipo de domínio; a tela/fluxo que registra quitações é escopo futuro,
 * fora de T7B.1-T7B.5). O tipo existe aqui porque `computeBalances` (`algorithm-spec.md`, T33)
 * recebe uma lista de `Quitacao`/`Settlement` além das despesas — sem o tipo, T33 não teria o que
 * consumir.
 */
data class Settlement(
    val id: String,
    val payerId: String,
    val receiverId: String,
    val amount: Money,
)
