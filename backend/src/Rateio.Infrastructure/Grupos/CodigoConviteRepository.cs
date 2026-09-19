using Microsoft.EntityFrameworkCore;
using Rateio.Application.Grupos;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Grupos;

/// <summary>
/// Implementação de <see cref="ICodigoConviteRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T21.1/T21.2).
/// </summary>
public sealed class CodigoConviteRepository(AppDbContext dbContext) : ICodigoConviteRepository
{
    /// <summary>
    /// Upsert por grupo (ver <c>CodigoConvite</c> — "um código ativo por grupo"): remove qualquer
    /// código anterior do mesmo <see cref="CodigoConvite.GrupoId"/> antes de inserir o novo, numa
    /// única transação implícita do <see cref="DbContext.SaveChangesAsync(CancellationToken)"/>.
    /// </summary>
    public async Task SalvarAsync(CodigoConvite codigo, CancellationToken cancellationToken = default)
    {
        var codigosAnteriores = await dbContext.CodigosConvite
            .Where(entidade => entidade.GrupoId == codigo.GrupoId)
            .ToListAsync(cancellationToken);

        dbContext.CodigosConvite.RemoveRange(codigosAnteriores);
        dbContext.CodigosConvite.Add(ConstruirEntidade(codigo));

        await dbContext.SaveChangesAsync(cancellationToken);
    }

    public async Task<CodigoConvite?> ObterPorCodigoAsync(string codigo, CancellationToken cancellationToken = default)
    {
        var entidade = await dbContext.CodigosConvite
            .AsNoTracking()
            .SingleOrDefaultAsync(c => c.Codigo == codigo, cancellationToken);

        return entidade is null ? null : ParaModeloDeAplicacao(entidade);
    }

    private static CodigoConviteEntity ConstruirEntidade(CodigoConvite codigo) => new()
    {
        Codigo = codigo.Valor,
        GrupoId = codigo.GrupoId,
        CriadoEm = codigo.CriadoEm,
        ExpiraEm = codigo.ExpiraEm,
    };

    private static CodigoConvite ParaModeloDeAplicacao(CodigoConviteEntity entidade) =>
        new(entidade.Codigo, entidade.GrupoId, entidade.CriadoEm, entidade.ExpiraEm);
}
