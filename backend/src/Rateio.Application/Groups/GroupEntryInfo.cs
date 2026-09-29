using Rateio.Domain;

namespace Rateio.Application.Groups;

/// <summary>
/// Minimal read of an existing group, originally used only by
/// <see cref="JoinGroupViaInviteUseCase"/> (T21.2) to reconstruct enough of a domain
/// <see cref="Group"/> to call <see cref="Group.AddParticipant"/> (T15) — which needs the current
/// participant list to revalidate the "no duplicate participant" invariant.
///
/// Not the full aggregate (no expenses, no real <see cref="Group.Synced"/>): the only use
/// <see cref="JoinGroupViaInviteUseCase"/> makes of the <see cref="Group"/> rebuilt from this is
/// calling <c>AddParticipant</c> and discarding the rest — actually persisting the join uses
/// <see cref="IGroupRepository.AddParticipantAsync"/> directly with the group's real
/// <see cref="Guid"/> (same pattern as <c>Rateio.Infrastructure.Expenses.ExpenseRepository</c>:
/// inserts via the FK without reloading the whole aggregate).
///
/// T28.3 reuses this same type for half of <see cref="FullGroup"/> (name/category/participants of
/// <c>GET /groups/{id}</c>) instead of introducing a second read with the same shape — the caller
/// already knows the group's real <see cref="Guid"/> from the route/parameter, so this type doesn't
/// need to carry the id.
/// </summary>
public sealed record GroupEntryInfo(GroupName Name, GroupCategory Category, IReadOnlyList<Participant> Participants);
