using Even.Domain;

namespace Even.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a group. <see cref="Category"/> reuses the domain enum directly (without
/// duplicating it as a string/second enum): it's a closed value with no behavior, so there's no
/// risk of divergence like the one avoided by not duplicating the expense split type.
/// </summary>
public class GroupEntity
{
    public Guid Id { get; set; }

    public string Name { get; set; } = string.Empty;

    public GroupCategory Category { get; set; }

    /// <summary>true as soon as the group goes through <c>POST /groups/sync</c> —
    /// from then on the server is the source of truth for this group.</summary>
    public bool Synced { get; set; }

    /// <summary>
    /// Owner/admin of the group. Only the authenticated user themself can sync on their own
    /// behalf — never taken from the request body, always from the JWT's <c>sub</c> claim.
    /// </summary>
    public Guid OwnerUserId { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public List<ParticipantEntity> Participants { get; set; } = [];

    public List<ExpenseEntity> Expenses { get; set; } = [];

    public UserEntity? Owner { get; set; }
}
