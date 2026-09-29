namespace Tally.Domain;

/// <summary>
/// Contract for the debt simplification engine (see algorithm-spec.md). This is the project's
/// technical core (constitution, principle 4) — exposed as an interface so that whoever consumes
/// it (application/API layer) depends on an abstraction, not the concrete implementation. This
/// makes it easy to test in isolation and allows swapping the settlement strategy later without
/// breaking callers.
/// </summary>
public interface IDebtSimplificationEngine
{
    /// <summary>
    /// Recomputes each participant's net balance from scratch, from the full history of expenses
    /// and settlements to date. Positive balance = owed to them; negative = they owe; zero =
    /// settled. The sum of all returned balances is always zero.
    /// </summary>
    IReadOnlyDictionary<ParticipantId, Money> ComputeBalances(
        IReadOnlyList<Expense> expenses,
        IReadOnlyList<Settlement> settlements);

    /// <summary>
    /// Given each participant's net balance, produces the minimal-enough list of transactions
    /// (greedy algorithm) that zeroes out all balances.
    /// </summary>
    IReadOnlyList<Transaction> ComputeSettlement(IReadOnlyDictionary<ParticipantId, Money> balances);
}
