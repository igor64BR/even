namespace Tally.Application.Notifications;

/// <summary>
/// Real-time notification abstraction (T38, constitution.md principle 3) — Application only
/// depends on this, never directly on <c>Microsoft.AspNetCore.SignalR</c> (Dependency Inversion:
/// the transport detail flows from the outside in, not the other way around). The real
/// implementation with <c>IHubContext&lt;TallyHub&gt;</c> lives in <c>Tally.Api</c> (where the
/// Hub also lives — see <c>Tally.Api.Hubs.SignalRGroupEventNotifier</c>); use case tests use a
/// mock/fake of this interface, without needing a real Hub running.
///
/// A failed notification must never block the business operation that was already persisted
/// successfully — it's the implementation's responsibility (not this contract's) to decide how to
/// handle a delivery failure (e.g. log and swallow), because RF35/RF36 is "notify in real time",
/// not "guarantee delivery": the kill-state limitation (constitution.md principle 3) already
/// documents that delivery via the Hub isn't guaranteed, and the pull fallback (T39) covers what
/// gets lost.
/// </summary>
public interface IGroupEventNotifier
{
    Task NotifyAsync(IGroupEvent groupEvent, CancellationToken cancellationToken = default);
}
