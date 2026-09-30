package com.tally.domain.engine

import com.tally.domain.model.Expense
import com.tally.domain.model.Money
import com.tally.domain.model.Settlement
import com.tally.domain.model.SettlementSuggestion

/**
 * Contract for the debt-simplification engine. This is the project's technical core — exposed as
 * an interface so that consumers (the "Settle debts" screen) depend on an abstraction, not the
 * concrete implementation. This makes isolated testing easier and lets the settlement strategy
 * change later without breaking callers.
 *
 * Mirrors `IDebtSimplificationEngine` (`backend/src/Tally.Domain/IDebtSimplificationEngine.cs`)
 * — same two-function contract, same sign rule, same greedy algorithm. Both implementations (this
 * one and the C# one) need to produce exactly the same result for the same input.
 */
interface DebtSimplificationEngine {

    /**
     * Recomputes each participant's net balance from scratch, from the full history of expenses
     * and settlements in effect. Positive balance = owed to them; negative = they owe; zero =
     * settled. The sum of all returned balances is always zero — the engine neither creates nor
     * destroys money.
     */
    fun computeBalances(expenses: List<Expense>, settlements: List<Settlement>): Map<String, Money>

    /**
     * Given each participant's net balance, produces the minimal-enough list of transactions
     * (greedy algorithm: the largest debtor pays the largest creditor, reselected on every
     * iteration) that zeroes out every balance.
     */
    fun computeSettlement(balances: Map<String, Money>): List<SettlementSuggestion>
}
