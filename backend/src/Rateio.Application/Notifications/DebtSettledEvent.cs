namespace Rateio.Application.Notifications;

/// <summary>
/// Raised by <see cref="Settlements.RegisterSettlementUseCase"/> (T35) after successfully
/// persisting (T38.3). Carries from/to/amount — enough for the app to reflect the settlement in
/// the UI without an extra round trip (RNF10: delivery in &lt;=2s); the next read of
/// <c>GET /groups/{id}/settlement</c> remains the recomputed source of truth.
/// </summary>
public sealed record DebtSettledEvent(
    Guid GroupId,
    Guid SettlementId,
    Guid FromParticipantId,
    Guid ToParticipantId,
    long AmountCents) : IGroupEvent
{
    public GroupEventType Type => GroupEventType.DebtSettled;
}
