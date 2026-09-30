namespace Tally.Application.Groups;

/// <summary>
/// Centralized access check: "the group exists, and the authenticated user is its owner" —
/// checked before any operation on behalf of a user over a specific group
/// (<see cref="GroupNotFoundException"/> if the group doesn't exist, <see cref="AccessDeniedException"/>
/// if it exists but doesn't belong to the user). Extracted from <c>CreateExpenseUseCase</c>
/// to be reused by <c>Tally.Application.Simplification.GetDebtSimplificationUseCase</c> —
/// avoids the same access check being copied by any future "per group" use case.
/// </summary>
internal static class GroupAccessVerification
{
    public static async Task<GroupAccess> RequireAsync(
        IGroupRepository groupRepository,
        Guid groupId,
        Guid authenticatedUserId,
        CancellationToken cancellationToken)
    {
        var access = await groupRepository.GetAccessAsync(groupId, cancellationToken)
            ?? throw new GroupNotFoundException(groupId);

        if (!access.BelongsTo(authenticatedUserId))
        {
            throw new AccessDeniedException(authenticatedUserId, access.GroupId);
        }

        return access;
    }
}
