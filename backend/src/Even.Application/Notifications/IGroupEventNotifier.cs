namespace Even.Application.Notifications;

/// <summary>
/// Real-time notification abstraction — Application only
/// depends on this, never directly on <c>Microsoft.AspNetCore.SignalR</c> (Dependency Inversion:
/// the transport detail flows from the outside in, not the other way around). The real
/// implementation with <c>IHubContext&lt;EvenHub&gt;</c> lives in <c>Even.Api</c> (where the
/// Hub also lives — see <c>Even.Api.Hubs.SignalRGroupEventNotifier</c>); use case tests use a
/// mock/fake of this interface, without needing a real Hub running.
///
/// A failed notification must never block the business operation that was already persisted
/// successfully — it's the implementation's responsibility (not this contract's) to decide how to
/// handle a delivery failure (e.g. log and swallow), because the goal is to notify in real time,
/// not to guarantee delivery: a dropped connection means the client misses the event, and the
/// pull fallback covers what gets lost.
/// </summary>
public interface IGroupEventNotifier
{
    Task NotifyAsync(IGroupEvent groupEvent, CancellationToken cancellationToken = default);
}
