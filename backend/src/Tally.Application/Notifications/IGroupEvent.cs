namespace Tally.Application.Notifications;

/// <summary>
/// Common contract for real-time group events (T38, RF35/RF36) — each concrete event
/// (<see cref="ExpenseCreatedEvent"/>, <see cref="DebtSettledEvent"/>) is its own type, never a
/// loose <c>object</c>/<c>dynamic</c> passed along. <see cref="GroupId"/> is what
/// <see cref="IGroupEventNotifier"/> uses to pick the destination SignalR group without needing a
/// redundant second parameter on every call.
/// </summary>
public interface IGroupEvent
{
    GroupEventType Type { get; }

    Guid GroupId { get; }
}
