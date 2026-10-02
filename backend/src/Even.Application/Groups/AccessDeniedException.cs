namespace Even.Application.Groups;

/// <summary>
/// The authenticated user does not have access to the group (see <see cref="GroupAccess"/>
/// for which link is checked today). Never becomes a 500 — the controller maps this to 403.
/// </summary>
public sealed class AccessDeniedException(Guid userId, Guid groupId)
    : Exception($"User {userId} does not have access to group {groupId}.")
{
    public Guid UserId { get; } = userId;

    public Guid GroupId { get; } = groupId;
}
