using Microsoft.EntityFrameworkCore;
using Rateio.Application.Despesas;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence;

namespace Rateio.Infrastructure.Despesas;

/// <summary>
/// Implementação de <see cref="IDespesaRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T23, T32). Insere a despesa direto pela FK (<c>DespesaEntity.GrupoId</c>) em vez de carregar o
/// <c>GrupoEntity</c> inteiro (participantes/despesas existentes) só pra anexar mais uma despesa —
/// o grupo já foi confirmado como existente por <c>IGrupoRepository.ObterAcessoAsync</c> antes deste
/// método ser chamado (<see cref="CriarDespesaUseCase"/>).
/// </summary>
public sealed class DespesaRepository(AppDbContext dbContext) : IDespesaRepository
{
    public async Task AdicionarAsync(
        Guid grupoId, DespesaParaPersistir despesa, CancellationToken cancellationToken = default)
    {
        var entidade = MapeadorDeDespesaEntity.Construir(despesa);
        entidade.GrupoId = grupoId;

        dbContext.Despesas.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    /// <summary>
    /// T32: leitura <c>AsNoTracking</c> (somente leitura) de todas as despesas vigentes do grupo,
    /// com <c>Include</c> das participações — sem elas o motor de simplificação (T31) não
    /// conseguiria reconstruir a divisão de cada despesa. Acesso ao grupo já foi validado por quem
    /// chama (<c>ObterSimplificacaoDeDividasUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<Despesa>> ObterPorGrupoAsync(
        Guid grupoId, CancellationToken cancellationToken = default)
    {
        var entidades = await dbContext.Despesas
            .AsNoTracking()
            .Include(despesa => despesa.Participacoes)
            .Where(despesa => despesa.GrupoId == grupoId)
            .ToListAsync(cancellationToken);

        return entidades.Select(MapeadorDeDespesaParaDominio.Construir).ToList();
    }

    /// <summary>
    /// T39.1: leitura <c>AsNoTracking</c> projetada direto na consulta (sem <c>Include</c> de
    /// participações — o fallback de pull não precisa da divisão da despesa, só do resumo que
    /// compõe <see cref="Notificacoes.EventoDespesaCriada"/>), filtrada por grupo e por
    /// <c>CriadoEm</c> depois de <paramref name="desde"/>. Acesso ao grupo já foi validado por quem
    /// chama (<c>Notificacoes.ObterEventosDeGrupoUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<DespesaOcorrida>> ObterOcorridasDesdeAsync(
        Guid grupoId, DateTimeOffset desde, CancellationToken cancellationToken = default)
    {
        return await dbContext.Despesas
            .AsNoTracking()
            .Where(despesa => despesa.GrupoId == grupoId && despesa.CriadoEm > desde)
            .Select(despesa => new DespesaOcorrida(
                despesa.Id,
                despesa.Descricao,
                despesa.ValorTotalCentavos,
                despesa.PagadorId,
                despesa.CriadoEm))
            .ToListAsync(cancellationToken);
    }
}
