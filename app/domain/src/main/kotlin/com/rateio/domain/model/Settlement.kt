package com.rateio.domain.model

/**
 * Registro de que [payerId] pagou [amount] a [receiverId] para quitar (parte de) uma dívida
 * existente (RF31/RF33). Espelha `Quitacao` (`backend/src/Rateio.Domain/Quitacao.cs`).
 *
 * [groupId] existe desde T42.1 — persistência real via `SettlementRepository`
 * (`:domain`)/`RoomSettlementRepository` (`:data`), mesmo padrão de [Expense.groupId]: toda
 * leitura é sempre escopada a um grupo (a tela "Quitar dívidas" só se importa com as quitações do
 * grupo que está olhando). Antes de T42.1 este tipo só existia para `computeBalances`
 * (`algorithm-spec.md`, T33) ter o que consumir — não tinha DAO/entidade Room própria.
 */
data class Settlement(
    val id: String,
    val groupId: String,
    val payerId: String,
    val receiverId: String,
    val amount: Money,
)
