namespace Even.Application.Notifications;

/// <summary>
/// Raised by <see cref="Expenses.CreateExpenseUseCase"/> after successfully persisting.
/// Carries enough for the app to update its UI without an extra round trip (delivered in under 2s)
/// — description, total amount, and who paid, not the full split list (that
/// still comes from <c>GET /groups/{id}/settlement</c> on demand).
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
