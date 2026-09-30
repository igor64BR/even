namespace Tally.Domain;

/// <summary>
/// Reference implementation of <see cref="IDebtSimplificationEngine"/>.
/// </summary>
public sealed class DebtSimplificationEngine : IDebtSimplificationEngine
{
    public IReadOnlyDictionary<ParticipantId, Money> ComputeBalances(
        IReadOnlyList<Expense> expenses,
        IReadOnlyList<Settlement> settlements)
    {
        var balances = new Dictionary<ParticipantId, Money>();

        foreach (var expense in expenses)
            ApplyExpense(balances, expense);

        foreach (var settlement in settlements)
            ApplySettlement(balances, settlement);

        return balances;
    }

    public IReadOnlyList<Transaction> ComputeSettlement(IReadOnlyDictionary<ParticipantId, Money> balances)
    {
        var debtors = ParticipantsWithNegativeBalance(balances);
        var creditors = ParticipantsWithPositiveBalance(balances);
        var transactions = new List<Transaction>();

        while (debtors.Count > 0 && creditors.Count > 0)
            transactions.Add(SettleLargestDebtorWithLargestCreditor(debtors, creditors));

        return transactions;
    }

    // --- computeBalances -----------------------------------------------------------------

    private static void ApplyExpense(Dictionary<ParticipantId, Money> balances, Expense expense)
    {
        Credit(balances, expense.PayerId, expense.TotalAmount);

        var shares = Split(expense.TotalAmount, expense.Splits);
        foreach (var (participantId, share) in shares)
            Debit(balances, participantId, share);
    }

    private static void ApplySettlement(Dictionary<ParticipantId, Money> balances, Settlement settlement)
    {
        Credit(balances, settlement.PayerId, settlement.Amount);
        Debit(balances, settlement.PayeeId, settlement.Amount);
    }

    private static void Credit(Dictionary<ParticipantId, Money> balances, ParticipantId id, Money amount) =>
        balances[id] = CurrentBalance(balances, id) + amount;

    private static void Debit(Dictionary<ParticipantId, Money> balances, ParticipantId id, Money amount) =>
        balances[id] = CurrentBalance(balances, id) - amount;

    private static Money CurrentBalance(Dictionary<ParticipantId, Money> balances, ParticipantId id) =>
        balances.TryGetValue(id, out var balance) ? balance : Money.Zero;

    // --- expense split (largest-remainder allocation) -------------------------------------

    private static IReadOnlyDictionary<ParticipantId, Money> Split(
        Money total, IReadOnlyList<ExpenseSplit> splits)
    {
        if (splits.Count == 0)
            throw new ArgumentException("Expense with no participants in the split.", nameof(splits));

        return splits[0] switch
        {
            ExpenseSplit.Equal => SplitEqually(total, splits),
            ExpenseSplit.Weighted => SplitByWeight(total, splits),
            ExpenseSplit.FixedAmount => SplitByFixedAmount(splits),
            var split => throw new NotSupportedException(
                $"Unsupported split type: {split.GetType().Name}")
        };
    }

    private static IReadOnlyDictionary<ParticipantId, Money> SplitEqually(
        Money total, IReadOnlyList<ExpenseSplit> splits)
    {
        var sorted = splits
            .Select(split => split.ParticipantId)
            .OrderBy(id => id)
            .ToList();

        var count = sorted.Count;
        var baseCents = total.Cents / count;
        var remainder = total.Cents % count;

        var shares = new Dictionary<ParticipantId, Money>();
        for (var i = 0; i < count; i++)
        {
            var extraCent = i < remainder ? 1 : 0;
            shares[sorted[i]] = Money.FromCents(baseCents + extraCent);
        }

        return shares;
    }

    private static IReadOnlyDictionary<ParticipantId, Money> SplitByWeight(
        Money total, IReadOnlyList<ExpenseSplit> splits)
    {
        var weighted = splits.Cast<ExpenseSplit.Weighted>().ToList();
        var totalWeight = weighted.Sum(w => w.Weight);

        var baseShareById = weighted.ToDictionary(
            w => w.ParticipantId,
            w => total.Cents * w.Weight / totalWeight);
        var fractionalRemainderById = weighted.ToDictionary(
            w => w.ParticipantId,
            w => total.Cents * w.Weight % totalWeight);

        var undistributedCents = total.Cents - baseShareById.Values.Sum();
        var tieBreakOrder = weighted
            .Select(w => w.ParticipantId)
            .OrderByDescending(id => fractionalRemainderById[id])
            .ThenBy(id => id)
            .ToList();

        var shares = new Dictionary<ParticipantId, Money>();
        for (var i = 0; i < tieBreakOrder.Count; i++)
        {
            var id = tieBreakOrder[i];
            var extraCent = i < undistributedCents ? 1 : 0;
            shares[id] = Money.FromCents(baseShareById[id] + extraCent);
        }

        return shares;
    }

    private static IReadOnlyDictionary<ParticipantId, Money> SplitByFixedAmount(
        IReadOnlyList<ExpenseSplit> splits) =>
        splits
            .Cast<ExpenseSplit.FixedAmount>()
            .ToDictionary(s => s.ParticipantId, s => s.Amount);

    // --- computeSettlement (greedy algorithm) ----------------------------------------------

    private static Transaction SettleLargestDebtorWithLargestCreditor(
        List<ParticipantWithBalance> debtors, List<ParticipantWithBalance> creditors)
    {
        var debtor = RemoveLargest(debtors);
        var creditor = RemoveLargest(creditors);

        var settledAmount = SmallerAmount(debtor.Remaining, creditor.Remaining);

        ReturnIfStillHasBalance(debtors, debtor.WithRemaining(debtor.Remaining - settledAmount));
        ReturnIfStillHasBalance(creditors, creditor.WithRemaining(creditor.Remaining - settledAmount));

        return new Transaction(debtor.Id, creditor.Id, settledAmount);
    }

    /// <summary>
    /// Finds and removes the largest remaining balance from the list: amount desc, tie broken by
    /// ParticipantId asc. Re-selecting the largest on every call (instead of sorting once and
    /// walking with pointers) is intentional.
    /// </summary>
    private static ParticipantWithBalance RemoveLargest(List<ParticipantWithBalance> participants)
    {
        var largest = participants
            .OrderByDescending(participant => participant.Remaining)
            .ThenBy(participant => participant.Id)
            .First();

        participants.Remove(largest);
        return largest;
    }

    private static void ReturnIfStillHasBalance(
        List<ParticipantWithBalance> participants, ParticipantWithBalance participant)
    {
        if (!participant.Remaining.IsPositive) return;
        participants.Add(participant);
    }

    private static Money SmallerAmount(Money a, Money b) => a.CompareTo(b) <= 0 ? a : b;

    private static List<ParticipantWithBalance> ParticipantsWithNegativeBalance(
        IReadOnlyDictionary<ParticipantId, Money> balances) =>
        balances
            .Where(pair => pair.Value.IsNegative)
            .Select(pair => new ParticipantWithBalance(pair.Key, -pair.Value))
            .ToList();

    private static List<ParticipantWithBalance> ParticipantsWithPositiveBalance(
        IReadOnlyDictionary<ParticipantId, Money> balances) =>
        balances
            .Where(pair => pair.Value.IsPositive)
            .Select(pair => new ParticipantWithBalance(pair.Key, pair.Value))
            .ToList();

    /// <summary>Participant with the (always positive) amount still left to settle in the
    /// greedy algorithm's debtor or creditor queue.</summary>
    private sealed record ParticipantWithBalance(ParticipantId Id, Money Remaining)
    {
        public ParticipantWithBalance WithRemaining(Money newRemaining) => this with { Remaining = newRemaining };
    }
}
