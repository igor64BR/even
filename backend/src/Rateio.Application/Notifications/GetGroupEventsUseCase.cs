using Rateio.Application.Expenses;
using Rateio.Application.Groups;
using Rateio.Application.Settlements;

namespace Rateio.Application.Notifications;

/// <summary>
/// T39.1: <c>GET /groups/{id}/events?since={iso8601Timestamp}</c> — pull fallback
/// (constitution.md principle 3: no third-party push, the app only receives events in real time
/// while the SignalR connection (T38) is active; on reconnecting, it needs to recover what it
/// missed). There's no events table: expenses and settlements already have a server timestamp
/// (<c>CreatedAt</c>) good enough to reconstruct "what happened in this group since X" — this use
/// case just queries both, each through its own read projection
/// (<see cref="ExpenseOccurred"/>/<see cref="SettlementOccurred"/>, exposed by the already-existing
/// repositories), builds the same event type T38 already uses in real time
/// (<see cref="ExpenseCreatedEvent"/>/<see cref="DebtSettledEvent"/>) and returns everything
/// interleaved, sorted by <c>CreatedAt</c> ascending.
///
/// Same access pattern as <c>Simplification.GetDebtSimplificationUseCase</c> (T32) and
/// <c>Settlements.RegisterSettlementUseCase</c> (T35): validates RNF07 via
/// <see cref="GroupAccessVerification"/> before any read —
/// <see cref="GroupNotFoundException"/> if the group doesn't exist (404, mapped in the controller),
/// <see cref="AccessDeniedException"/> if it exists but the authenticated user doesn't have access
/// to it (403).
/// </summary>
public sealed class GetGroupEventsUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository,
    ISettlementRepository settlementRepository)
{
    public async Task<IReadOnlyList<IGroupEvent>> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        DateTimeOffset since,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var expenses = await expenseRepository.GetOccurredSinceAsync(groupId, since, cancellationToken);
        var settlements = await settlementRepository.GetOccurredSinceAsync(groupId, since, cancellationToken);

        var expenseEvents = expenses.Select(expense => new
        {
            Event = (IGroupEvent)new ExpenseCreatedEvent(
                groupId, expense.Id, expense.Description, expense.TotalAmountCents, expense.PayerId),
            expense.CreatedAt,
        });

        var settlementEvents = settlements.Select(settlement => new
        {
            Event = (IGroupEvent)new DebtSettledEvent(
                groupId, settlement.Id, settlement.PayerId, settlement.PayeeId, settlement.AmountCents),
            settlement.CreatedAt,
        });

        return expenseEvents
            .Concat(settlementEvents)
            .OrderBy(item => item.CreatedAt)
            .Select(item => item.Event)
            .ToList();
    }
}
