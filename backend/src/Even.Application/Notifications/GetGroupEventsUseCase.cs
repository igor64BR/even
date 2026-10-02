using Even.Application.Expenses;
using Even.Application.Groups;
using Even.Application.Settlements;

namespace Even.Application.Notifications;

/// <summary>
/// <c>GET /groups/{id}/events?since={iso8601Timestamp}</c> — pull fallback: the app only receives
/// events in real time while the SignalR connection is active; on reconnecting, it needs to
/// recover what it missed. There's no events table: expenses and settlements already have a
/// server timestamp (<c>CreatedAt</c>) good enough to reconstruct "what happened in this group
/// since X" — this use case just queries both, each through its own read projection
/// (<see cref="ExpenseOccurred"/>/<see cref="SettlementOccurred"/>, exposed by the already-existing
/// repositories), builds the same event type used in real time
/// (<see cref="ExpenseCreatedEvent"/>/<see cref="DebtSettledEvent"/>) and returns everything
/// interleaved, sorted by <c>CreatedAt</c> ascending.
///
/// Same access pattern as <c>Simplification.GetDebtSimplificationUseCase</c> and
/// <c>Settlements.RegisterSettlementUseCase</c>: validates access via
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
