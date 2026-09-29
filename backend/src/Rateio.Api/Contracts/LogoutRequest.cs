namespace Rateio.Api.Contracts;

/// <summary>
/// Body of <c>POST /auth/logout</c>. The refresh token goes in the body — not read from the
/// <c>Authorization</c> header — because it, not the access token, is what identifies the session
/// to revoke (it's what <see cref="Rateio.Application.Auth.IRefreshTokenRepository"/> stores the
/// hash of). This also lets logout work even with an already-expired access token (RNF06: up to 15
/// min), exactly the common case of "user opened the app after a while and wants to sign out".
/// </summary>
public sealed record LogoutRequest(string RefreshToken);
