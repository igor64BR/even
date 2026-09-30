using Tally.Application.Groups;
using Tally.Domain;

namespace Tally.Api.Contracts;

/// <summary>
/// Success response of <c>GET /groups/{id}</c>: <c>POST /groups/join/{code}</c>
/// (<see cref="SyncGroupResponse"/>-like) only returns
/// <c>{groupId}</c>, with no way for the app to fetch the rest of the group's data after joining
/// via a link.
///
/// Same shape already established by the sync payload (<c>SyncGroupRequest</c>: participant
/// with id/name/guest-or-not, expense with description/amount/payer/date/split) — deliberately not
/// a parallel response shape, just the output mirror of the same contract that already comes in
/// through <c>POST /groups/sync</c>.
/// </summary>
public sealed record GetGroupResponse(
    string Name,
    GroupCategory Category,
    IReadOnlyList<ParticipantResponse> Participants,
    IReadOnlyList<DetailedExpenseResponse> Expenses);

/// <summary>Group participant — id/name/guest-or-not, same as the entry edge.</summary>
public sealed record ParticipantResponse(Guid Id, string Name, bool IsGuest);

/// <summary>
/// Group expense with everything the app needs to populate the screen: description, amount, payer,
/// entry date, and the full split among participants.
/// </summary>
public sealed record DetailedExpenseResponse(
    Guid Id,
    string Description,
    long TotalAmountCents,
    Guid PayerId,
    DateOnly Date,
    SplitTypeRequest SplitType,
    IReadOnlyList<ExpenseSplitResponse> Splits);

/// <summary>
/// A participant's share of an expense. <see cref="Weight"/>/<see cref="AmountCents"/> are only
/// populated when <see cref="DetailedExpenseResponse.SplitType"/> is, respectively,
/// <see cref="SplitTypeRequest.Weighted"/>/<see cref="SplitTypeRequest.FixedAmount"/> — same
/// convention as <c>SyncedExpenseSplitRequest</c>.
/// </summary>
public sealed record ExpenseSplitResponse(Guid ParticipantId, long? Weight, long? AmountCents);
