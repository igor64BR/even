using Microsoft.EntityFrameworkCore;
using Rateio.Application.Quitacoes;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence;

namespace Rateio.Infrastructure.Quitacoes;

/// <summary>
/// Implementação de <see cref="IQuitacaoRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T32) — mesmo padrão de <c>Rateio.Infrastructure.Despesas.DespesaRepository</c>: leitura
/// <c>AsNoTracking</c> (somente leitura), filtrada por <c>GrupoId</c>, cujo acesso já foi validado
/// por quem chama (<c>ObterSimplificacaoDeDividasUseCase</c>) antes deste método ser invocado.
/// </summary>
public sealed class QuitacaoRepository(AppDbContext dbContext) : IQuitacaoRepository
{
    public async Task<IReadOnlyList<Quitacao>> ObterPorGrupoAsync(
        Guid grupoId, CancellationToken cancellationToken = default)
    {
        var entidades = await dbContext.Quitacoes
            .AsNoTracking()
            .Where(quitacao => quitacao.GrupoId == grupoId)
            .ToListAsync(cancellationToken);

        return entidades.Select(MapeadorDeQuitacaoParaDominio.Construir).ToList();
    }
}
