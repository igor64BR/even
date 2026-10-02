package com.even.domain.repository

import com.even.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting a group's settlements. `:domain` declares it, `:data` implements it with
 * Room (Dependency Inversion) — same pattern as [ExpenseRepository]/[GroupRepository]. Consumed by
 * the "Settle debts" screen: every "Mark as paid" records a new [Settlement];
 * [getSettlementsFlow] feeds back into `DebtSimplificationEngine.computeBalances`, which
 * recomputes the balances and, as a result, the suggestion list.
 */
interface SettlementRepository {
    fun getSettlementsFlow(groupId: String): Flow<List<Settlement>>
    suspend fun insertSettlement(settlement: Settlement)
}
