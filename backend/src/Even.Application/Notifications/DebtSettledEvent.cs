namespace Even.Application.Notifications;

/// <summary>
/// Raised by <see cref="Settlements.RegisterSettlementUseCase"/> after successfully
/// persisting. Carries from/to/amount — enough for the app to reflect the settlement in
/// the UI without an extra round trip (delivered in under 2s); the next read of
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
