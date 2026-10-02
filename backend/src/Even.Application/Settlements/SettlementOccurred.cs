namespace Even.Application.Settlements;

/// <summary>
/// Read projection for the pull fallback (<c>GET /groups/{id}/events</c>,
/// <see cref="Notifications.GetGroupEventsUseCase"/>) — same role as
/// <see cref="Expenses.ExpenseOccurred"/>, but for settlements: enough to build a
/// <see cref="Notifications.DebtSettledEvent"/> plus <see cref="CreatedAt"/> (server timestamp),
/// used both to filter "since" and to sort chronologically alongside expenses in the use case.
/// Distinct from <see cref="Even.Domain.Settlement"/> (the simplification engine doesn't use
/// a timestamp).
/// </summary>
public sealed record SettlementOccurred(
    Guid Id,
    Guid PayerId,
    Guid PayeeId,
    long AmountCents,
    DateTimeOffset CreatedAt);
