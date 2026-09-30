using Tally.Domain;

namespace Tally.Application.Groups;

/// <summary>
/// Payload of <c>POST /groups/sync</c>: the full local state of a group (Room, app) that
/// has never touched the backend yet. Carries no "owner"/"user" field at all — whoever syncs is
/// always the authenticated user from the JWT itself, never a value received in the
/// request body, so there's no way for one user to sync on behalf of another here.
///
/// Lives in Application (not Api.Contracts) because it's consumed directly by
/// <see cref="SyncGroupUseCase"/> — see <see cref="SyncedGroupBuilder"/> for the DTO-to-domain
/// mapping.
/// </summary>
public sealed record SyncGroupRequest(
    string Name,
    GroupCategory Category,
    IReadOnlyList<SyncedParticipantRequest> Participants,
    IReadOnlyList<SyncedExpenseRequest> Expenses);

/// <summary>
/// Participant of the local group. <see cref="Id"/> is the stable identifier already assigned by
/// the app (Room) — it needs to be preserved as-is, because <see cref="SyncedExpenseRequest.PayerId"/>
/// and <see cref="SyncedExpenseSplitRequest.ParticipantId"/> reference this same value elsewhere in
/// the payload.
/// </summary>
public sealed record SyncedParticipantRequest(Guid Id, string Name, bool IsGuest);

/// <summary>
/// Expense logged locally. <see cref="SplitType"/> is per-expense (not per-split) — it reflects
/// the domain rule that all splits of a given expense are of the same concrete
/// <see cref="ExpenseSplit"/> subtype (see that type's comment); validating/enforcing that
/// consistency at the entry edge is exactly this DTO's job.
/// </summary>
public sealed record SyncedExpenseRequest(
    Guid Id,
    string Description,
    long TotalAmountCents,
    Guid PayerId,
    DateOnly Date,
    SplitTypeRequest SplitType,
    IReadOnlyList<SyncedExpenseSplitRequest> Splits);

/// <summary>Mirrors the three concrete subtypes of <see cref="ExpenseSplit"/>.</summary>
public enum SplitTypeRequest
{
    Equal,
    Weighted,
    FixedAmount,
}

/// <summary>
/// A participant's share of an expense. <see cref="Weight"/> is only required when
/// <see cref="SyncedExpenseRequest.SplitType"/> is <see cref="SplitTypeRequest.Weighted"/>;
/// <see cref="AmountCents"/> only when it's <see cref="SplitTypeRequest.FixedAmount"/>. For
/// <see cref="SplitTypeRequest.Equal"/> neither is used.
/// </summary>
public sealed record SyncedExpenseSplitRequest(Guid ParticipantId, long? Weight, long? AmountCents);
