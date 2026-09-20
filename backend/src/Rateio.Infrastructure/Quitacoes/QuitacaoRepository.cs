using Microsoft.EntityFrameworkCore;
using Rateio.Application.Quitacoes;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence;

namespace Rateio.Infrastructure.Quitacoes;

/// <summary>
/// Implementação de <see cref="IQuitacaoRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T32/T35) — mesmo padrão de <c>Rateio.Infrastructure.Despesas.DespesaRepository</c>: leitura
/// <c>AsNoTracking</c> (somente leitura), filtrada por <c>GrupoId</c>, cujo acesso já foi validado
/// por quem chama (<c>ObterSimplificacaoDeDividasUseCase</c>/<c>RegistrarQuitacaoUseCase</c>) antes
/// de qualquer um destes métodos ser invocado.
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

    /// <summary>
    /// T35: insere a quitação direto pela FK (<c>QuitacaoEntity.GrupoId</c>), sem carregar o
    /// <c>GrupoEntity</c> inteiro só pra anexar mais uma linha — mesma decisão de
    /// <c>DespesaRepository.AdicionarAsync</c> (T23), pelo mesmo motivo: o grupo já foi confirmado
    /// como existente por <c>IGrupoRepository.ObterAcessoAsync</c> antes deste método ser chamado.
    /// </summary>
    public async Task AdicionarAsync(Guid grupoId, Quitacao quitacao, CancellationToken cancellationToken = default)
    {
        var entidade = MapeadorDeQuitacaoEntity.Construir(quitacao);
        entidade.GrupoId = grupoId;

        dbContext.Quitacoes.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
