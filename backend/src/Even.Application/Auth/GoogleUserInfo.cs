namespace Even.Application.Auth;

/// <summary>
/// Data extracted from the Google ID token after the signature and audience have already been
/// validated. Carries no secret (never the token itself) — only what the backend needs to
/// identify/create the local user.
/// </summary>
public sealed record GoogleUserInfo(string GoogleSubjectId, string Name, string Email);
