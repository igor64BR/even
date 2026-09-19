using Rateio.Application.Grupos;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Grupos;

/// <summary>
/// Implementação de <see cref="IGrupoRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T18.1). Mapeamento Domínio→EF em métodos pequenos e nomeados, espelhando
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

    private static List<DespesaEntity> ConstruirEntidadesDespesas(IReadOnlyList<DespesaParaSincronizar> despesas) =>
        despesas.Select(ConstruirEntidadeDespesa).ToList();

    private static DespesaEntity ConstruirEntidadeDespesa(DespesaParaSincronizar despesaParaSincronizar) => new()
    {
        Id = despesaParaSincronizar.Despesa.Id,
        PagadorId = despesaParaSincronizar.Despesa.PagadorId.Valor,
        ValorTotalCentavos = despesaParaSincronizar.Despesa.ValorTotal.Centavos,
        Descricao = despesaParaSincronizar.Descricao,
        Data = despesaParaSincronizar.Data,
        CriadoEm = DateTimeOffset.UtcNow,
        Participacoes = ConstruirEntidadesParticipacoes(despesaParaSincronizar.Despesa.Participacoes),
    };

    private static List<ParticipacaoDespesaEntity> ConstruirEntidadesParticipacoes(
        IReadOnlyList<ParticipacaoDespesa> participacoes) =>
        participacoes.Select(ConstruirEntidadeParticipacao).ToList();

    private static ParticipacaoDespesaEntity ConstruirEntidadeParticipacao(ParticipacaoDespesa participacao) =>
        participacao switch
        {
            ParticipacaoDespesa.PorIgual porIgual => new ParticipacaoDespesaEntity
            {
                Id = Guid.NewGuid(),
                ParticipanteId = porIgual.ParticipanteId.Valor,
                Tipo = TipoDivisaoEntity.PorIgual,
            },
            ParticipacaoDespesa.PorPeso porPeso => new ParticipacaoDespesaEntity
            {
                Id = Guid.NewGuid(),
                ParticipanteId = porPeso.ParticipanteId.Valor,
                Tipo = TipoDivisaoEntity.PorPeso,
                Peso = porPeso.Peso,
            },
            ParticipacaoDespesa.PorValorFixo porValorFixo => new ParticipacaoDespesaEntity
            {
                Id = Guid.NewGuid(),
                ParticipanteId = porValorFixo.ParticipanteId.Valor,
                Tipo = TipoDivisaoEntity.PorValorFixo,
                ValorCentavos = porValorFixo.Valor.Centavos,
            },
            var naoSuportada => throw new NotSupportedException(
                $"Tipo de participação não suportado: {naoSuportada.GetType().Name}"),
        };
}
