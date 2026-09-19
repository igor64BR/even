using Microsoft.EntityFrameworkCore;
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

    public async Task RevogarAsync(
        string refreshToken,
        CancellationToken cancellationToken = default)
    {
        var tokenHash = RefreshTokenHasher.Hash(refreshToken);

        // Busca pelo hash (índice único, ver RefreshTokenEntityConfiguration) — o valor em texto
        // puro do refresh token nunca é usado em where/log, só pra derivar o hash acima.
        var entidade = await dbContext.RefreshTokens
            .SingleOrDefaultAsync(refreshTokenEntity => refreshTokenEntity.TokenHash == tokenHash, cancellationToken);

        if (entidade is null || entidade.RevogadoEm is not null)
        {
            // Token desconhecido ou já revogado: idempotente, não é erro (ver doc de
            // IRefreshTokenRepository.RevogarAsync).
            return;
        }

        entidade.RevogadoEm = DateTimeOffset.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
