using Microsoft.EntityFrameworkCore;
using Rateio.Application.Auth;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementação de <see cref="IUsuarioRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// </summary>
public sealed class UsuarioRepository(AppDbContext dbContext) : IUsuarioRepository
{
    public async Task<Usuario?> ObterPorGoogleSubjectIdAsync(
        string googleSubjectId,
        CancellationToken cancellationToken = default)
    {
        var entidade = await dbContext.Usuarios
            .AsNoTracking()
            .SingleOrDefaultAsync(usuario => usuario.GoogleSubjectId == googleSubjectId, cancellationToken);

        return entidade is null ? null : ParaModeloDeAplicacao(entidade);
    }

    public async Task<Usuario> CriarAsync(GoogleUserInfo dadosGoogle, CancellationToken cancellationToken = default)
    {
        var entidade = new UsuarioEntity
        {
            Id = Guid.NewGuid(),
            GoogleSubjectId = dadosGoogle.GoogleSubjectId,
            Nome = dadosGoogle.Nome,
            Email = dadosGoogle.Email,
            CriadoEm = DateTimeOffset.UtcNow,
        };

        dbContext.Usuarios.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);

        return ParaModeloDeAplicacao(entidade);
    }

    private static Usuario ParaModeloDeAplicacao(UsuarioEntity entidade) =>
        new(entidade.Id, entidade.GoogleSubjectId, entidade.Nome, entidade.Email);
}
