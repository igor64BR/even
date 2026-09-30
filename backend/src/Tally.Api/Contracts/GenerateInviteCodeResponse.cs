namespace Tally.Api.Contracts;

/// <summary>
/// Success response of <c>POST /groups/{id}/invite-code</c>. <see cref="ExpiresAt"/> makes
/// explicit to the app when the code stops being valid — see
/// <c>Tally.Application.Groups.InviteCode</c> for the expiration/reuse policy.
/// </summary>
public sealed record GenerateInviteCodeResponse(string Code, DateTimeOffset ExpiresAt);
