namespace Tally.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a user authenticated via Google (T11). Never stores a password —
/// authentication is entirely delegated to Google (constitution.md, principle 2); the only
/// identity-related secret this schema stores is the refresh token's hash, in
/// <see cref="RefreshTokenEntity"/>.
/// </summary>
public class UserEntity
{
    public Guid Id { get; set; }

    public string GoogleSubjectId { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public DateTimeOffset CreatedAt { get; set; }

    public List<RefreshTokenEntity> RefreshTokens { get; set; } = [];
}
