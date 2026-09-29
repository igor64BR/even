namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// A refresh session issued for a user (T11). Stores only the refresh token's hash — never the
/// plain-text value — so that a database leak doesn't expose usable session tokens.
/// <see cref="RevokedAt"/> already exists (even with nothing filling it in yet) so T14 (logout)
/// doesn't need another migration just for that.
/// </summary>
public class RefreshTokenEntity
{
    public Guid Id { get; set; }

    public Guid UserId { get; set; }

    public string TokenHash { get; set; } = string.Empty;

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset ExpiresAt { get; set; }

    public DateTimeOffset? RevokedAt { get; set; }

    public UserEntity? User { get; set; }
}
