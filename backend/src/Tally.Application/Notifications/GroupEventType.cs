namespace Tally.Application.Notifications;

/// <summary>
/// Discriminates the kind of <see cref="IGroupEvent"/> without resorting to
/// <c>object</c>/<c>dynamic</c> or loose type-checking (<c>is</c>/pattern matching) in every
/// consumer. Each value's name is the same one used as the SignalR method name
/// the client listens to (<c>connection.on("ExpenseCreated", ...)</c>), but the field itself serves
/// any consumer without access to the "method name" (e.g. the pull fallback, if it ever comes to
/// persist the event).
/// </summary>
public enum GroupEventType
{
    ExpenseCreated,
    DebtSettled,
}
