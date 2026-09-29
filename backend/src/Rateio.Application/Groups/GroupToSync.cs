using Rateio.Application.Expenses;
using Rateio.Domain;

namespace Rateio.Application.Groups;

/// <summary>
/// Output of the DTO-to-domain mapping (<see cref="SyncedGroupBuilder"/>): the already-validated
/// <see cref="Group"/> aggregate (T15's invariants applied via <see cref="Group.Create"/>), along
/// with the id of the authenticated user who will be the owner (RNF07 — never from the payload,
/// only from the JWT) and the group's expenses (<see cref="ExpenseToPersist"/> — the same type T23
/// uses for a single expense, see that type's comment for why it's shared).
/// </summary>
public sealed record GroupToSync(
    Group Group,
    Guid OwnerUserId,
    IReadOnlyList<ExpenseToPersist> Expenses);
