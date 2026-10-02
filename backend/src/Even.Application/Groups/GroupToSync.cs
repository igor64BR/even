using Even.Application.Expenses;
using Even.Domain;

namespace Even.Application.Groups;

/// <summary>
/// Output of the DTO-to-domain mapping (<see cref="SyncedGroupBuilder"/>): the already-validated
/// <see cref="Group"/> aggregate (invariants applied via <see cref="Group.Create"/>), along
/// with the id of the authenticated user who will be the owner (never from the payload,
/// only from the JWT) and the group's expenses (<see cref="ExpenseToPersist"/> — the same type
/// used for a single expense, see that type's comment for why it's shared).
/// </summary>
public sealed record GroupToSync(
    Group Group,
    Guid OwnerUserId,
    IReadOnlyList<ExpenseToPersist> Expenses);
