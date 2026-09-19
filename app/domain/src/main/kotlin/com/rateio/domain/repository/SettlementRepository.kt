package com.rateio.domain.repository

import com.rateio.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistência de quitações (RF31/RF33) de um grupo. `:domain` declara, `:data`
 * implementa com Room (Dependency Inversion) — mesmo padrão de [ExpenseRepository]/
 * [GroupRepository]. Consumido pela tela "Quitar dívidas" (T42.3): cada "Marcar como pago" grava
 * um [Settlement] novo; [getSettlementsFlow] realimenta `DebtSimplificationEngine.computeBalances`
 * (T33), que recalcula os saldos e, por consequência, a lista de sugestões.
 */
interface SettlementRepository {
    fun getSettlementsFlow(groupId: String): Flow<List<Settlement>>
    suspend fun insertSettlement(settlement: Settlement)
}
