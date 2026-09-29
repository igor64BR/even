namespace Rateio.Application.Groups;

/// <summary>
/// T21.1: generates the invite code for a synced group. Reuses
/// <see cref="IGroupRepository.GetAccessAsync"/> (the same one used by
/// <c>Rateio.Application.Expenses.CreateExpenseUseCase</c>, T23) to validate the group's existence
/// and owner — <see cref="GroupAccess.BelongsTo"/> today IS the owner check (see that type's
/// comment), so "owner only" (RNF07/T21) is exactly the same rule, with no new concept needed.
///
/// Validation order (same pattern as T23): (1) group exists, otherwise
/// <see cref="GroupNotFoundException"/> (404); (2) authenticated user is the owner, otherwise
/// <see cref="AccessDeniedException"/> (403 — "generate code, owner only", unlike "join via code",
/// which T21.2 opens to any authenticated user).
/// </summary>
public sealed class GenerateInviteCodeUseCase(
    IGroupRepository groupRepository,
    IInviteCodeRepository inviteCodeRepository,
    TimeProvider clock)
{
    private static readonly TimeSpan CodeValidity = TimeSpan.FromDays(7);

    public async Task<InviteCode> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        CancellationToken cancellationToken = default)
    {
        var access = await GetAccessOrFail(groupId, cancellationToken);
        RequireOwner(access, authenticatedUserId);

        var code = GenerateCode(groupId);

        await inviteCodeRepository.SaveAsync(code, cancellationToken);

        return code;
    }

    private async Task<GroupAccess> GetAccessOrFail(Guid groupId, CancellationToken cancellationToken) =>
        await groupRepository.GetAccessAsync(groupId, cancellationToken)
            ?? throw new GroupNotFoundException(groupId);

    private static void RequireOwner(GroupAccess access, Guid authenticatedUserId)
    {
        if (!access.BelongsTo(authenticatedUserId))
        {
            throw new AccessDeniedException(authenticatedUserId, access.GroupId);
        }
    }

    private InviteCode GenerateCode(Guid groupId)
    {
        var now = clock.GetUtcNow();

        return new InviteCode(InviteCodeGenerator.Generate(), groupId, now, now + CodeValidity);
    }
}
