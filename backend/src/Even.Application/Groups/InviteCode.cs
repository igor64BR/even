namespace Even.Application.Groups;

/// <summary>
/// Invite code for a synced group. Product decisions documented here because there's
/// no other obvious place for them to live:
///
/// <list type="bullet">
/// <item><b>Reusable, not single-use.</b> It's the group's link/invite mechanism — several
/// people should be able to join with the same code, otherwise the owner would need to generate
/// (and redistribute) a new code for every invitee, which isn't the "group link" mental
/// model.</item>
/// <item><b>One active code per group.</b> Generating a new code (calling the endpoint again)
/// replaces the previous one (<see cref="IInviteCodeRepository.SaveAsync"/> upserts by
/// <see cref="GroupId"/>) — avoids old, forgotten-but-still-valid codes circulating, and gives the
/// owner a simple way to "revoke" a compromised link: generate another one.</item>
/// <item><b>Expires in 7 days.</b> It doesn't need to be single-use to be secure enough for an MVP,
/// but a code with no expiry at all
/// would stay valid forever if leaked. 7 days is plenty of time for an invite to reach the invitee
/// and for them to join, without the code becoming a very-long-lived secret.</item>
/// </list>
/// </summary>
public sealed record InviteCode(string Value, Guid GroupId, DateTimeOffset CreatedAt, DateTimeOffset ExpiresAt)
{
    public bool IsValid(DateTimeOffset now) => now < ExpiresAt;
}
