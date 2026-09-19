using Microsoft.EntityFrameworkCore;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Domain;
using Rateio.Infrastructure.Despesas;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Grupos;

/// <summary>
/// Implementação de <see cref="IGrupoRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T18.1, T23.2). Mapeamento Domínio→EF em métodos pequenos e nomeados, espelhando
/// <c>ConstrutorDeGrupoSincronizado</c> (Application) do lado oposto da tradução. Não seta
/// manualmente <c>GrupoId</c>/<c>DespesaId</c> nas entidades filhas — isso é resolvido pelo EF via
/// fixup de relacionamento a partir das coleções de navegação, do mesmo jeito que as
/// configurations em <c>Persistence/Configurations</c> já declaram.
/// </summary>
public sealed class GrupoRepository(AppDbContext dbContext) : IGrupoRepository
{
    public async Task<Guid> SincronizarAsync(GrupoParaSincronizar grupo, CancellationToken cancellationToken = default)
    {
        var entidade = ConstruirEntidadeGrupo(grupo);

        dbContext.Grupos.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);

        return entidade.Id;
    }

    /// <summary>
    /// T23.2: leitura mínima (sem participantes/despesas) só pra validar existência + RNF07 antes
    /// de adicionar uma despesa avulsa — <c>AsNoTracking</c> porque é somente leitura.
    /// </summary>
    public Task<AcessoAoGrupo?> ObterAcessoAsync(Guid grupoId, CancellationToken cancellationToken = default) =>
        dbContext.Grupos
            .AsNoTracking()
            .Where(grupo => grupo.Id == grupoId)
            .Select(grupo => new AcessoAoGrupo(grupo.Id, grupo.DonoUsuarioId))
            .FirstOrDefaultAsync(cancellationToken);

    /// <summary>
    /// T21.2: leitura mínima (nome/categoria/participantes, sem despesas) pra
    /// <c>EntrarNoGrupoViaConviteUseCase</c> reconstruir o agregado de domínio antes de chamar
    /// <c>Grupo.AdicionarParticipante</c> — <c>AsNoTracking</c> porque é somente leitura, o insert
    /// de fato acontece em <see cref="AdicionarParticipanteAsync"/>.
    /// </summary>
    public async Task<GrupoParaEntrada?> ObterParaEntradaAsync(Guid grupoId, CancellationToken cancellationToken = default)
    {
        var entidade = await dbContext.Grupos
            .AsNoTracking()
            .Include(grupo => grupo.Participantes)
            .SingleOrDefaultAsync(grupo => grupo.Id == grupoId, cancellationToken);

        return entidade is null ? null : ParaGrupoParaEntrada(entidade);
    }

    /// <summary>
    /// T21.2: insere o participante direto pela FK (<c>GrupoId</c>), sem recarregar o
    /// <see cref="GrupoEntity"/> inteiro — mesmo padrão de
    /// <c>Rateio.Infrastructure.Despesas.DespesaRepository.AdicionarAsync</c> (T23.2).
    /// </summary>
    public Task AdicionarParticipanteAsync(Guid grupoId, Participante participante, CancellationToken cancellationToken = default)
    {
        dbContext.Participantes.Add(new ParticipanteEntity
        {
            Id = participante.Id.Valor,
            GrupoId = grupoId,
            Nome = participante.Nome.Valor,
            EhConvidado = participante.EhConvidado,
        });

        return dbContext.SaveChangesAsync(cancellationToken);
    }

    private static GrupoParaEntrada ParaGrupoParaEntrada(GrupoEntity entidade) => new(
        NomeGrupo.Criar(entidade.Nome),
        entidade.Categoria,
        entidade.Participantes.Select(ParaParticipanteDeDominio).ToList());

    private static Participante ParaParticipanteDeDominio(ParticipanteEntity entidade)
    {
        var id = new ParticipanteId(entidade.Id);
        var nome = NomeParticipante.Criar(entidade.Nome);

        return entidade.EhConvidado
            ? Participante.Convidado(id, nome)
            : Participante.Autenticado(id, nome);
    }

    private static GrupoEntity ConstruirEntidadeGrupo(GrupoParaSincronizar grupoParaSincronizar) => new()
    {
        Id = grupoParaSincronizar.Grupo.Id,
        Nome = grupoParaSincronizar.Grupo.Nome.Valor,
        Categoria = grupoParaSincronizar.Grupo.Categoria,
        Sincronizado = grupoParaSincronizar.Grupo.Sincronizado,
        DonoUsuarioId = grupoParaSincronizar.DonoUsuarioId,
        CriadoEm = DateTimeOffset.UtcNow,
        Participantes = ConstruirEntidadesParticipantes(grupoParaSincronizar.Grupo.Participantes),
        Despesas = ConstruirEntidadesDespesas(grupoParaSincronizar.Despesas),
    };

    private static List<ParticipanteEntity> ConstruirEntidadesParticipantes(
        IReadOnlyList<Participante> participantes) =>
        participantes.Select(ConstruirEntidadeParticipante).ToList();

    private static ParticipanteEntity ConstruirEntidadeParticipante(Participante participante) => new()
    {
        Id = participante.Id.Valor,
        Nome = participante.Nome.Valor,
        EhConvidado = participante.EhConvidado,
    };

    // T23 extraiu a construção da DespesaEntity em si (incluindo participações) pra
    // Rateio.Infrastructure.Despesas.MapeadorDeDespesaEntity — reaproveitada aqui e pelo novo
    // DespesaRepository, em vez de duplicada.
    private static List<DespesaEntity> ConstruirEntidadesDespesas(IReadOnlyList<DespesaParaPersistir> despesas) =>
        despesas.Select(MapeadorDeDespesaEntity.Construir).ToList();
}
