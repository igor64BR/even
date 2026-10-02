namespace Even.Application.Groups;

/// <summary>
/// First sync of a local group to the backend. Builds a domain
/// <see cref="Domain.Group"/>/<see cref="Domain.Participant"/>/<see cref="Domain.Expense"/> from
/// the payload (via <see cref="SyncedGroupBuilder"/>, which reuses <see cref="Domain.Group.Create"/>
/// — doesn't bypass the group's invariant), marks the group as synced (from then on, the server
/// becomes the source of truth) and persists via <see cref="IGroupRepository"/>.
///
/// <paramref name="authenticatedUserId"/> never comes from the request body — the controller
/// extracts it from the validated JWT's <c>sub</c> claim. That's how it's structurally
/// guaranteed: there's no "owner" field in the payload, so there's no way for a user to sync on
/// behalf of another.
/// </summary>
public sealed class SyncGroupUseCase(IGroupRepository groupRepository)
{
    public Task<Guid> ExecuteAsync(
        Guid authenticatedUserId,
        SyncGroupRequest request,
        CancellationToken cancellationToken = default)
    {
        var groupToSync = SyncedGroupBuilder.Build(authenticatedUserId, request);
        groupToSync.Group.MarkAsSynced();

        return groupRepository.SyncAsync(groupToSync, cancellationToken);
    }
}
