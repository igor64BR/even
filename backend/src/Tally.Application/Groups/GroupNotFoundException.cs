namespace Tally.Application.Groups;

/// <summary>
/// The referenced group (e.g. <c>POST /groups/{id}/expenses</c>) does not exist on the
/// backend. Never becomes a 500 — the controller maps it to 404 (same pattern as
/// <c>InvalidGoogleTokenException</c> becoming a 401 in <c>AuthController</c>).
/// </summary>
public sealed class GroupNotFoundException(Guid groupId) : Exception($"Group {groupId} not found.")
{
    public Guid GroupId { get; } = groupId;
}
