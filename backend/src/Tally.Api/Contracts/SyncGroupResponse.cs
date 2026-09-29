namespace Tally.Api.Contracts;

/// <summary>
/// Success response of <c>POST /groups/sync</c>. <see cref="GroupId"/> is the group's id on the
/// server — never the local Room id; the app stores this as the local group's "remote id" (scope of
/// T19, see T18-backend-sync-grupo.md).
/// </summary>
public sealed record SyncGroupResponse(Guid GroupId);
