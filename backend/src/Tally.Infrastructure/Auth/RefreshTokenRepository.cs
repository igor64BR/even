using Microsoft.EntityFrameworkCore;
using Tally.Application.Auth;
using Tally.Infrastructure.Persistence;
using Tally.Infrastructure.Persistence.Entities;

namespace Tally.Infrastructure.Auth;

/// <summary>
/// Implementation of <see cref="IRefreshTokenRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// Stores only the refresh token's hash — never the received value (see
/// <see cref="RefreshTokenHasher"/>).
/// </summary>
public sealed class RefreshTokenRepository(AppDbContext dbContext) : IRefreshTokenRepository
{
    public async Task SaveAsync(
        Guid userId,
        string refreshToken,
        DateTimeOffset expiresAt,
        CancellationToken cancellationToken = default)
    {
        var entity = new RefreshTokenEntity
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TokenHash = RefreshTokenHasher.Hash(refreshToken),
            CreatedAt = DateTimeOffset.UtcNow,
            ExpiresAt = expiresAt,
        };

        dbContext.RefreshTokens.Add(entity);
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    public async Task RevokeAsync(
        string refreshToken,
        CancellationToken cancellationToken = default)
    {
        var tokenHash = RefreshTokenHasher.Hash(refreshToken);

        // Look up by hash (unique index, see RefreshTokenEntityConfiguration) — the refresh
        // token's plain-text value is never used in a where/log, only to derive the hash above.
        var entity = await dbContext.RefreshTokens
            .SingleOrDefaultAsync(refreshTokenEntity => refreshTokenEntity.TokenHash == tokenHash, cancellationToken);

        if (entity is null || entity.RevokedAt is not null)
        {
            // Unknown or already-revoked token: idempotent, not an error (see
            // IRefreshTokenRepository.RevokeAsync's doc).
            return;
        }

        entity.RevokedAt = DateTimeOffset.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
