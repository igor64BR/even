package com.tally.domain.engine

import com.tally.domain.model.Expense
import com.tally.domain.model.ExpenseSplit
import com.tally.domain.model.Money
import com.tally.domain.model.Settlement
import com.tally.domain.model.SettlementSuggestion

/**
 * Reference implementation of [DebtSimplificationEngine]: a line-by-line translation of the
 * pseudocode in `algorithm-spec.md`. Direct port of `DebtSimplificationEngine`
 * (`backend/src/Tally.Domain/DebtSimplificationEngine.cs`, T31) — same sign rule, same greedy
 * algorithm with the largest debtor/creditor reselected on every iteration (not two pointers over
 * lists sorted once — see the caveat in the `computeSettlement` section of `algorithm-spec.md`),
 * same largest-remainder method for closing splits that don't divide evenly.
 */
class GreedyDebtSimplificationEngine : DebtSimplificationEngine {

    override fun computeBalances(expenses: List<Expense>, settlements: List<Settlement>): Map<String, Money> {
        val balances = mutableMapOf<String, Money>()

        expenses.forEach { expense -> applyExpense(balances, expense) }
        settlements.forEach { settlement -> applySettlement(balances, settlement) }

        return balances
    }

    override fun computeSettlement(balances: Map<String, Money>): List<SettlementSuggestion> {
        val debtors = participantsWithNegativeBalance(balances).toMutableList()
        val creditors = participantsWithPositiveBalance(balances).toMutableList()
        val transactions = mutableListOf<SettlementSuggestion>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            transactions += settleLargestDebtorWithLargestCreditor(debtors, creditors)
        }

        return transactions
    }

    // --- computeBalances -----------------------------------------------------------------

    private fun applyExpense(balances: MutableMap<String, Money>, expense: Expense) {
        val total = Money.ofCents(expense.amountCents)
        credit(balances, expense.paidByParticipantId, total)

        val shares = split(total, expense.splits)
        shares.forEach { (participantId, share) -> debit(balances, participantId, share) }
    }

    private fun applySettlement(balances: MutableMap<String, Money>, settlement: Settlement) {
        credit(balances, settlement.payerId, settlement.amount)
        debit(balances, settlement.receiverId, settlement.amount)
    }

    private fun credit(balances: MutableMap<String, Money>, participantId: String, amount: Money) {
        balances[participantId] = currentBalance(balances, participantId) + amount
    }

    private fun debit(balances: MutableMap<String, Money>, participantId: String, amount: Money) {
        balances[participantId] = currentBalance(balances, participantId) - amount
    }

    private fun currentBalance(balances: Map<String, Money>, participantId: String): Money =
        balances[participantId] ?: Money.ZERO

    // --- expense split (closed off with the largest-remainder method) --------------------

    private fun split(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        require(splits.isNotEmpty()) { "Expense has no participants in the split." }

        return when (splits.first()) {
            is ExpenseSplit.Equal -> splitEqually(total, splits)
            is ExpenseSplit.Weight -> splitByWeight(total, splits)
            is ExpenseSplit.FixedAmount -> splitByFixedAmount(splits)
        }
    }

    private fun splitEqually(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        val sortedIds = splits.map { it.participantId }.sorted()

        val count = sortedIds.size
        val base = total.cents / count
        val remainder = total.cents % count

        return sortedIds.mapIndexed { index, participantId ->
            val extraCent = if (index < remainder) 1 else 0
            participantId to Money.ofCents(base + extraCent)
        }.toMap()
    }

    private fun splitByWeight(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        val weights = splits.filterIsInstance<ExpenseSplit.Weight>()
        val totalWeight = weights.sumOf { it.weight }

        val baseShareById = weights.associate { it.participantId to (total.cents * it.weight / totalWeight) }
        val fractionalRemainderById = weights.associate { it.participantId to (total.cents * it.weight % totalWeight) }

        val undistributedCents = total.cents - baseShareById.values.sum()
        val tieBreakOrder = weights
            .map { it.participantId }
            .sortedWith(compareByDescending<String> { fractionalRemainderById.getValue(it) }.thenBy { it })

        return tieBreakOrder.mapIndexed { index, participantId ->
            val extraCent = if (index < undistributedCents) 1 else 0
            participantId to Money.ofCents(baseShareById.getValue(participantId) + extraCent)
        }.toMap()
    }

    private fun splitByFixedAmount(splits: List<ExpenseSplit>): Map<String, Money> =
        splits.filterIsInstance<ExpenseSplit.FixedAmount>()
            .associate { it.participantId to it.amount }

    // --- computeSettlement (greedy algorithm) ---------------------------------------------

    private fun settleLargestDebtorWithLargestCreditor(
        debtors: MutableList<ParticipantWithBalance>,
        creditors: MutableList<ParticipantWithBalance>,
    ): SettlementSuggestion {
        val debtor = removeLargestDebtorOrCreditor(debtors)
        val creditor = removeLargestDebtorOrCreditor(creditors)

        val settledAmount = smallerAmount(debtor.remaining, creditor.remaining)

        returnIfStillHasBalance(debtors, debtor.withRemaining(debtor.remaining - settledAmount))
        returnIfStillHasBalance(creditors, creditor.withRemaining(creditor.remaining - settledAmount))

        return SettlementSuggestion(debtor.id, creditor.id, settledAmount)
    }

    /**
     * Finds and removes the largest remaining balance from the list: amount desc, ties broken by
     * participantId asc. Reselecting the largest on every call (instead of sorting once and
     * walking with two pointers) is intentional — see algorithm-spec.md, computeSettlement
     * section: it's not a valid optimization of the pseudocode, it's a different algorithm that
     * can produce one extra transaction in some cases.
     */
    private fun removeLargestDebtorOrCreditor(participants: MutableList<ParticipantWithBalance>): ParticipantWithBalance {
        val largest = participants
            .sortedWith(compareByDescending<ParticipantWithBalance> { it.remaining }.thenBy { it.id })
            .first()

        participants.remove(largest)
        return largest
    }

    private fun returnIfStillHasBalance(participants: MutableList<ParticipantWithBalance>, participant: ParticipantWithBalance) {
        if (!participant.remaining.isPositive) return
        participants.add(participant)
    }

    private fun smallerAmount(a: Money, b: Money): Money = if (a <= b) a else b

    private fun participantsWithNegativeBalance(balances: Map<String, Money>): List<ParticipantWithBalance> =
        balances.filterValues { it.isNegative }.map { (id, balance) -> ParticipantWithBalance(id, -balance) }

    private fun participantsWithPositiveBalance(balances: Map<String, Money>): List<ParticipantWithBalance> =
        balances.filterValues { it.isPositive }.map { (id, balance) -> ParticipantWithBalance(id, balance) }

    /**
     * A participant with the (always positive) amount still owed in the greedy algorithm's debtor
     * or creditor queue. Mirrors `ParticipantWithBalance`, the equivalent private record in
     * `DebtSimplificationEngine.cs`.
     */
    private data class ParticipantWithBalance(val id: String, val remaining: Money) {
        fun withRemaining(newRemaining: Money): ParticipantWithBalance = copy(remaining = newRemaining)
    }
}
