using Rateio.Application.Despesas;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Despesas;

/// <summary>
/// Mapeamento Domínio→EF de uma despesa, extraído de
/// <c>Rateio.Infrastructure.Grupos.GrupoRepository</c> (T18.1) para ser reaproveitado por T23
/// (<see cref="DespesaRepository"/>), que persiste uma despesa avulsa num grupo já existente em vez
/// de um grafo de grupo inteiro. Espelha <c>Rateio.Application.Despesas.MapeadorDeDespesa</c> do
/// lado oposto da tradução (Object Calisthenics: métodos pequenos, nomeados, um por passo).
///
/// Não seta <see cref="DespesaEntity.GrupoId"/> — a entidade retornada ainda não está associada a
/// um grupo; quem chama decide como (fixup via coleção de navegação no bulk de sincronização, FK
/// direta no insert avulso de <see cref="DespesaRepository"/>).
/// </summary>
internal static class MapeadorDeDespesaEntity
{
    public static DespesaEntity Construir(DespesaParaPersistir despesaParaPersistir) => new()
    {
        Id = despesaParaPersistir.Despesa.Id,
        PagadorId = despesaParaPersistir.Despesa.PagadorId.Valor,
        ValorTotalCentavos = despesaParaPersistir.Despesa.ValorTotal.Centavos,
        Descricao = despesaParaPersistir.Descricao,
        Data = despesaParaPersistir.Data,
        CriadoEm = DateTimeOffset.UtcNow,
        Participacoes = ConstruirParticipacoes(despesaParaPersistir.Despesa.Participacoes),
    };

    private static List<ParticipacaoDespesaEntity> ConstruirParticipacoes(
        IReadOnlyList<ParticipacaoDespesa> participacoes) =>
        participacoes.Select(ConstruirParticipacao).ToList();

    private static ParticipacaoDespesaEntity ConstruirParticipacao(ParticipacaoDespesa participacao) =>
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
