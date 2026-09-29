using Tally.Application.Groups;
using Tally.Application.Notifications;

namespace Tally.Application.Settlements;

/// <summary>
/// T35.1: <c>POST /groups/{id}/settlements</c> — records that one of the transactions suggested by
/// <c>GET /groups/{id}/settlement</c> (T32) was paid (RF31/RF33). Same pattern as
/// <c>CreateExpenseUseCase</c> (T23): (1) validates group access via
/// <see cref="GroupAccessVerification"/> — <see cref="GroupNotFoundException"/> if the group
/// doesn't exist (404), <see cref="AccessDeniedException"/> if it exists but the authenticated
/// user doesn't have access to it (403, RNF07); only then (2) the payload becomes a domain
/// <see cref="Domain.Settlement"/> (T31) via <see cref="SettlementMapper"/> and (3) it's persisted
/// via <see cref="ISettlementRepository"/>. (4) T38.3: once the settlement is successfully
/// persisted, notify <see cref="IGroupEventNotifier"/> (RF35/RF36) — the same Application
/// abstraction used by <c>CreateExpenseUseCase</c>, never SignalR directly.
///
/// Does not recompute or store a balance (T35.2): the next call to
/// <c>GET /groups/{id}/settlement</c> already recomputes on demand from the current history of
/// expenses and settlements — the same guarantee <c>GetDebtSimplificationUseCase</c> (T32) already
/// offers.
///
/// <paramref name="authenticatedUserId"/> is only used to check access, never written as part of
/// the settlement — the same structural guarantee as <c>CreateExpenseUseCase</c>.
/// </summary>
public sealed class RegisterSettlementUseCase(
    IGroupRepository groupRepository,
    ISettlementRepository settlementRepository,
    IGroupEventNotifier groupEventNotifier)
{
    public async Task<Guid> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        RegisterSettlementRequest request,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var settlement = SettlementMapper.Build(request);

        await settlementRepository.AddAsync(groupId, settlement, cancellationToken);

        var settlementEvent = new DebtSettledEvent(
            groupId,
            settlement.Id,
            settlement.PayerId.Value,
            settlement.PayeeId.Value,
            settlement.Amount.Cents);
        await groupEventNotifier.NotifyAsync(settlementEvent, cancellationToken);

        return settlement.Id;
    }
}
