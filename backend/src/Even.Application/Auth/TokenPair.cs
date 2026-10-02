namespace Even.Application.Auth;

/// <summary>
/// Pair of tokens issued by the application after authenticating a <see cref="User"/>. The
/// access token is a short-lived JWT (expires in &lt;= 15 min); the refresh token is
/// opaque and stored (hashed) so it can be revoked later.
/// </summary>
public sealed record TokenPair(
    string AccessToken,
    DateTimeOffset AccessTokenExpiresAt,
    string RefreshToken,
    DateTimeOffset RefreshTokenExpiresAt);
