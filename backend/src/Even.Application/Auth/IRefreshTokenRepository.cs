namespace Even.Application.Auth;

/// <summary>
/// Persistence of issued sessions (refresh tokens), to allow future revocation. The
/// concrete implementation never stores the refresh token in plain text — only a hash of it.
/// </summary>
public interface IRefreshTokenRepository
{
    Task SaveAsync(
        Guid userId,
        string refreshToken,
        DateTimeOffset expiresAt,
        CancellationToken cancellationToken = default);

    /// <summary>
    /// Marks the corresponding session's revocation timestamp for
    /// <paramref name="refreshToken"/>. Idempotent: an already-revoked or unknown token is
    /// not an error, it just does nothing — this avoids exposing to the caller whether a given
    /// token ever existed.
    /// </summary>
    Task RevokeAsync(
        string refreshToken,
        CancellationToken cancellationToken = default);
}
