using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Application.Settlements;
using Tally.Domain;

namespace Tally.Application.Simplification;

/// <summary>
/// T32.1: <c>GET /groups/{id}/settlement</c> — loads the current history of expenses and
/// settlements for an already-synced group and delegates to the simplification engine (T31,
/// <see cref="IDebtSimplificationEngine"/>) the computation of balances and the list of suggested
/// transactions. This use case only orchestrates: access (RNF07), reads via the repositories, and
/// the two engine calls, in that order — no balance/settlement rule lives here, that's 100% T31's
/// responsibility (constitution, principle 4).
///
/// The result is never persisted: it's always recomputed from scratch from the current history
/// (the same guarantee <c>algorithm-spec.md</c> requires of <c>computeBalances</c>), so there's no
/// risk of a stale balance getting "stuck" in a cache.
/// </summary>
public sealed class GetDebtSimplificationUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository,
    ISettlementRepository settlementRepository,
    IDebtSimplificationEngine engine)
{
    public async Task<IReadOnlyList<Transaction>> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var expenses = await expenseRepository.GetByGroupAsync(groupId, cancellationToken);
        var settlements = await settlementRepository.GetByGroupAsync(groupId, cancellationToken);

        var balances = engine.ComputeBalances(expenses, settlements);
        return engine.ComputeSettlement(balances);
    }
}
