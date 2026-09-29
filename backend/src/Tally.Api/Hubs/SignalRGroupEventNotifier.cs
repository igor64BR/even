using Microsoft.AspNetCore.SignalR;
using Tally.Application.Notifications;

namespace Tally.Api.Hubs;

/// <summary>
/// T38.2/T38.3: real implementation of <see cref="IGroupEventNotifier"/> using
/// <see cref="IHubContext{TallyHub}"/> — lives in <c>Tally.Api</c> (not Tally.Infrastructure)
/// because that's where <see cref="TallyHub"/> also lives; nothing in Tally.Application
/// references <c>Microsoft.AspNetCore.SignalR</c> (Dependency Inversion: use cases depend only on
/// the abstraction, this class is the project's only point that concretely knows about SignalR in
/// order to emit events).
///
/// The SignalR method name sent to the group is <c>groupEvent.Type</c>
/// (<see cref="GroupEventType"/>) — <c>nameof(GroupEventType.ExpenseCreated)</c>/
/// <c>nameof(GroupEventType.DebtSettled)</c>, not the event class's name
/// (<c>nameof(ExpenseCreatedEvent)</c> would be <c>"ExpenseCreatedEvent"</c>, a different name) —
/// the client subscribes to exactly these names via
/// <c>connection.on("ExpenseCreated", ...)</c>/<c>connection.on("DebtSettled", ...)</c> (T40); a
/// method with the wrong name reaches the client as an unrecognized invocation (SignalR logs and
/// discards it instead of throwing), so this mismatch doesn't show up as a server-side failure —
/// only a manual end-to-end test (a real client receiving the event) catches it.
/// </summary>
public sealed class SignalRGroupEventNotifier(
    IHubContext<TallyHub> hubContext,
    ILogger<SignalRGroupEventNotifier> logger) : IGroupEventNotifier
{
    public async Task NotifyAsync(IGroupEvent groupEvent, CancellationToken cancellationToken = default)
    {
        try
        {
            var group = hubContext.Clients.Group(TallyHub.GroupName(groupEvent.GroupId));

            await (groupEvent switch
            {
                ExpenseCreatedEvent expenseCreated => group.SendAsync(
                    nameof(GroupEventType.ExpenseCreated), expenseCreated, cancellationToken),
                DebtSettledEvent debtSettled => group.SendAsync(
                    nameof(GroupEventType.DebtSettled), debtSettled, cancellationToken),
                _ => throw new ArgumentOutOfRangeException(
                    nameof(groupEvent), groupEvent.Type, "Group event type with no SignalR dispatch mapped."),
            });
        }
        catch (Exception error) when (error is not ArgumentOutOfRangeException)
        {
            // The business operation (expense/settlement) was already persisted successfully before
            // this call — a real-time delivery failure (e.g. hub unavailable) must never bring down
            // the use case. RF35/RF36 is "notify in real time", not "guarantee delivery"; the
            // kill-state/disconnection limitation is already a documented trade-off (constitution.md
            // principle 3), and the pull fallback (T39) covers what's lost here.
            logger.LogWarning(
                error,
                "Failed to notify event {EventType} of group {GroupId} via SignalR.",
                groupEvent.Type,
                groupEvent.GroupId);
        }
    }
}
