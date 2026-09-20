using Microsoft.EntityFrameworkCore;
using Rateio.Application.Despesas;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

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

    /// <summary>
    /// T28.3: mesma leitura <c>AsNoTracking</c> + <c>Include</c> de participações de
    /// <see cref="ObterPorGrupoAsync"/> (T32), só que reconstruindo <see cref="DespesaParaPersistir"/>
    /// (Despesa de domínio + Descrição + Data) em vez de só <see cref="Despesa"/> — reaproveita
    /// <see cref="MapeadorDeDespesaParaDominio"/> integralmente, sem duplicar a tradução EF→Domínio.
    /// Acesso ao grupo já foi validado por quem chama (<c>ObterGrupoUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<DespesaParaPersistir>> ObterDetalhadasPorGrupoAsync(
        Guid grupoId, CancellationToken cancellationToken = default)
    {
        var entidades = await dbContext.Despesas
            .AsNoTracking()
            .Include(despesa => despesa.Participacoes)
            .Where(despesa => despesa.GrupoId == grupoId)
            .ToListAsync(cancellationToken);

        return entidades
            .Select(entidade => new DespesaParaPersistir(
                MapeadorDeDespesaParaDominio.Construir(entidade), entidade.Descricao, entidade.Data))
            .ToList();
    }

    /// <summary>
    /// T28.1: carrega a entidade (com participações, pra poder substituí-las) filtrando por
    /// <paramref name="grupoId"/> — <c>false</c> sem checar/lançar nada quando ela não existe nesse
    /// grupo, quem chama (<c>EditarDespesaUseCase</c>) decide o que isso significa (404). Substitui
    /// os campos escalares e troca a coleção de participações inteira (nunca um merge item a item —
    /// mesma reconstrução completa que <c>MapeadorDeDespesa</c>/<c>CriarDespesaUseCase</c> já exigem
    /// do lado do domínio), reaproveitando <see cref="MapeadorDeDespesaEntity"/> pra montar as novas
    /// participações em vez de duplicar o switch por subtipo.
    /// </summary>
    public async Task<bool> AtualizarAsync(
        Guid grupoId, DespesaParaPersistir despesa, CancellationToken cancellationToken = default)
    {
        var entidadeExistente = await dbContext.Despesas
            .Include(d => d.Participacoes)
            .SingleOrDefaultAsync(d => d.GrupoId == grupoId && d.Id == despesa.Despesa.Id, cancellationToken);

        if (entidadeExistente is null)
        {
            return false;
        }

        AtualizarCamposEscalares(entidadeExistente, despesa);
        SubstituirParticipacoes(entidadeExistente, despesa);

        await dbContext.SaveChangesAsync(cancellationToken);
        return true;
    }

    /// <summary>
    /// T28.2: mesmo padrão de leitura filtrada por grupo de <see cref="AtualizarAsync"/> —
    /// <c>false</c> sem lançar quando a despesa não existe nesse grupo. As participações são
    /// removidas via cascade delete (<c>DespesaEntityConfiguration</c>), sem precisar carregá-las
    /// aqui.
    /// </summary>
    public async Task<bool> RemoverAsync(Guid grupoId, Guid despesaId, CancellationToken cancellationToken = default)
    {
        var entidade = await dbContext.Despesas
            .SingleOrDefaultAsync(d => d.GrupoId == grupoId && d.Id == despesaId, cancellationToken);

        if (entidade is null)
        {
            return false;
        }

        dbContext.Despesas.Remove(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);
        return true;
    }

    private static void AtualizarCamposEscalares(DespesaEntity entidade, DespesaParaPersistir despesa)
    {
        entidade.Descricao = despesa.Descricao;
        entidade.Data = despesa.Data;
        entidade.PagadorId = despesa.Despesa.PagadorId.Valor;
        entidade.ValorTotalCentavos = despesa.Despesa.ValorTotal.Centavos;
    }

    private void SubstituirParticipacoes(DespesaEntity entidade, DespesaParaPersistir despesa)
    {
        dbContext.ParticipacoesDeDespesa.RemoveRange(entidade.Participacoes);
        entidade.Participacoes = MapeadorDeDespesaEntity.Construir(despesa).Participacoes;
    }
}
