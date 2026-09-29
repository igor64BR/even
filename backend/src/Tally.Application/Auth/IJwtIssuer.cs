namespace Tally.Application.Auth;

/// <summary>
/// Abstracts issuing the application's token pair. The concrete implementation (JWT signing,
/// opaque refresh token generation) lives in Tally.Infrastructure — Application only knows this
/// interface, which allows testing issuance (expiration, distinction between tokens) without
/// spinning up a database or validating a real Google ID token.
/// </summary>
public interface IJwtIssuer
{
    /// <summary>
    /// Issues a new access token (short-lived, RNF06: &lt;= 15 min) and a new (opaque) refresh
    /// token for the given user. Persists nothing — persisting the refresh token (to allow
    /// revocation) is <see cref="IRefreshTokenRepository"/>'s responsibility.
    /// </summary>
    TokenPair Issue(User user);
}
