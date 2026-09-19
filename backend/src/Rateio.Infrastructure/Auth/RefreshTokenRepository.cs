using Rateio.Application.Auth;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementação de <see cref="IRefreshTokenRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// Guarda só o hash do refresh token — nunca o valor recebido (ver <see cref="RefreshTokenHasher"/>).
/// </summary>
public sealed class RefreshTokenRepository(AppDbContext dbContext) : IRefreshTokenRepository
{
    public async Task SalvarAsync(
        Guid usuarioId,
        string refreshToken,
        DateTimeOffset expiraEm,
        CancellationToken cancellationToken = default)
    {
        var entidade = new RefreshTokenEntity
        {
            Id = Guid.NewGuid(),
            UsuarioId = usuarioId,
            TokenHash = RefreshTokenHasher.Hash(refreshToken),
            CriadoEm = DateTimeOffset.UtcNow,
            ExpiraEm = expiraEm,
        };

        dbContext.RefreshTokens.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
