namespace Tally.Application.Groups;

/// <summary>
/// The invite code (<c>POST /groups/join/{code}</c>) doesn't exist, has already been
/// replaced by a newer code from the same group, or has expired (see <see cref="InviteCode"/>).
/// All three causes become the same 404 in the controller — from the outside, "invalid code" and
/// "code that never existed" shouldn't be distinguishable (don't leak whether a specific code ever
/// existed).
/// </summary>
public sealed class InvalidInviteCodeException(string code) : Exception($"Invite code '{code}' is invalid or expired.")
{
    public string Code { get; } = code;
}
