namespace Tally.Application.Notifications;

/// <summary>
/// Raised by <see cref="Expenses.CreateExpenseUseCase"/> (T23) after successfully persisting
/// (T38.2). Carries enough for the app to update its UI without an extra round trip (RNF10:
/// delivery in &lt;=2s) — description, total amount, and who paid, not the full split list (that
/// still comes from <c>GET /groups/{id}/settlement</c> on demand, T32).
/// </summary>
public sealed record ExpenseCreatedEvent(
    Guid GroupId,
    Guid ExpenseId,
    string Description,
    long TotalAmountCents,
    Guid PayerId) : IGroupEvent
{
    public GroupEventType Type => GroupEventType.ExpenseCreated;
}
