using Rateio.Domain;

namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a group. Minimal skeleton (T2.2) until T18: gained
/// <see cref="Category"/>, <see cref="Synced"/>, and <see cref="OwnerUserId"/> here because T18 is
/// the first time anything persists a full domain <see cref="Group"/> — before that nothing in the
/// project wrote these columns. <see cref="Category"/> reuses the domain enum directly (without
/// duplicating it as a string/second enum): it's a closed value with no behavior, so there's no
/// risk of divergence like the one that led T31 to not duplicate the expense split type.
/// </summary>
public class GroupEntity
{
    public Guid Id { get; set; }

    public string Name { get; set; } = string.Empty;

    public GroupCategory Category { get; set; }

    /// <summary>true as soon as the group goes through <c>POST /groups/sync</c> (RF09/T18.2) —
    /// from then on the server is the source of truth for this group.</summary>
    public bool Synced { get; set; }

    /// <summary>
    /// Owner/admin of the group (RNF07: only the authenticated user themself can sync on their own
    /// behalf — never taken from the request body, always from the JWT's <c>sub</c> claim).
    /// </summary>
    public Guid OwnerUserId { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public List<ParticipantEntity> Participants { get; set; } = [];

    public List<ExpenseEntity> Expenses { get; set; } = [];

    public UserEntity? Owner { get; set; }
}
