namespace Rateio.Application.Expenses;

/// <summary>
/// T39.1: read projection for the pull fallback (<c>GET /groups/{id}/events</c>,
/// <see cref="Notifications.GetGroupEventsUseCase"/>) — loads just enough to build an
/// <see cref="Notifications.ExpenseCreatedEvent"/> plus <see cref="CreatedAt"/>, the timestamp of
/// when the expense was persisted on the server. Distinct from <see cref="Rateio.Domain.Expense"/>
/// (T31: the simplification engine doesn't use description or timestamp) and from
/// <see cref="ExpenseToPersist"/> (which carries <c>Date</c>, the entry date reported by the app —
/// day granularity, not usable as a sync cursor). <see cref="CreatedAt"/> is the same field used
/// both to filter "since" and to sort chronologically alongside
/// <see cref="Settlements.SettlementOccurred"/> in the use case.
/// </summary>
public sealed record ExpenseOccurred(
    Guid Id,
    string Description,
    long TotalAmountCents,
    Guid PayerId,
    DateTimeOffset CreatedAt);
