using Rateio.Domain;

namespace Rateio.Application.Groups;

/// <summary>
/// T21.2: resolves an invite code to the corresponding group and adds the authenticated user as a
/// new authenticated <see cref="Participant"/> (RF07) — via
/// <see cref="Group.AddParticipant"/> (T15, already validates the "no duplicate participant"
/// invariant), not by building the persistence entity directly (that would bypass domain
/// validation, the same care <c>SyncedGroupBuilder</c> (T18) already takes).
///
/// RNF07/T21: unlike <see cref="GenerateInviteCodeUseCase"/> (owner only), joining via code is open
/// to any authenticated user — that's the point of the feature (invite link). That's why there's
/// no "owner" or <see cref="GroupAccess"/> check here: the valid code itself is the authorization.
///
/// <paramref name="authenticatedUserName"/> comes from the JWT's <c>name</c> claim (the controller
/// extracts it) — the same data <c>JwtIssuer</c> (T11) writes into the token from
/// <c>User.Name</c>, without needing a new query to <c>IUserRepository</c> just for this.
/// </summary>
public sealed class JoinGroupViaInviteUseCase(
    IInviteCodeRepository inviteCodeRepository,
    IGroupRepository groupRepository,
    TimeProvider clock)
{
    public async Task<Guid> ExecuteAsync(
        string code,
        string authenticatedUserName,
        CancellationToken cancellationToken = default)
    {
        var invite = await GetValidInviteOrFail(code, cancellationToken);
        var groupEntryInfo = await GetGroupOrFail(invite.GroupId, cancellationToken);

        var newParticipant = Participant.Authenticated(
            ParticipantId.New(),
            ParticipantName.Create(authenticatedUserName));

        // Reconstructs just enough of the aggregate to revalidate T15's invariant before
        // persisting — see GroupEntryInfo for why this doesn't need to be the full Group.
        var group = Group.Create(groupEntryInfo.Name, groupEntryInfo.Category, groupEntryInfo.Participants);
        group.AddParticipant(newParticipant);

        await groupRepository.AddParticipantAsync(invite.GroupId, newParticipant, cancellationToken);

        return invite.GroupId;
    }

    private async Task<InviteCode> GetValidInviteOrFail(string code, CancellationToken cancellationToken)
    {
        var invite = await inviteCodeRepository.GetByCodeAsync(code, cancellationToken)
            ?? throw new InvalidInviteCodeException(code);

        if (!invite.IsValid(clock.GetUtcNow()))
        {
            throw new InvalidInviteCodeException(code);
        }

        return invite;
    }

    private async Task<GroupEntryInfo> GetGroupOrFail(Guid groupId, CancellationToken cancellationToken) =>
        await groupRepository.GetForEntryAsync(groupId, cancellationToken)
            ?? throw new GroupNotFoundException(groupId);
}
