using Rateio.Domain;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Despesas;

/// <summary>
/// Mapeamento EF→Domínio de uma despesa (T32): a direção oposta de <see cref="MapeadorDeDespesaEntity"/>
/// (Domínio→EF, T23/T18.1). Até T32 nenhum caso de uso precisava reconstruir a
/// <see cref="Despesa"/> de domínio a partir do que está persistido — só persistir; o endpoint de
/// settlement (<c>GET /groups/{id}/settlement</c>) é o primeiro consumidor, porque o motor de
/// simplificação (T31) só entende o tipo de domínio, nunca <see cref="DespesaEntity"/>.
///
/// Reconstrói o subtipo concreto de <see cref="ParticipacaoDespesa"/> a partir do discriminador
/// <see cref="TipoDivisaoEntity"/> — o inverso exato de
/// <c>MapeadorDeDespesaEntity.ConstruirParticipacao</c>. Método por passo, nomeado, sem nenhum
/// método fazendo a tradução inteira de uma vez (mesmo Object Calisthenics que T23 já seguiu do
/// lado oposto).
/// </summary>
internal static class MapeadorDeDespesaParaDominio
{
    public static Despesa Construir(DespesaEntity entidade) => new(
        entidade.Id,
        Dinheiro.EmCentavos(entidade.ValorTotalCentavos),
        new ParticipanteId(entidade.PagadorId),
        ConstruirParticipacoes(entidade.Participacoes));

    private static List<ParticipacaoDespesa> ConstruirParticipacoes(
        IReadOnlyList<ParticipacaoDespesaEntity> participacoes) =>
        participacoes.Select(ConstruirParticipacao).ToList();

    private static ParticipacaoDespesa ConstruirParticipacao(ParticipacaoDespesaEntity entidade)
    {
        var participanteId = new ParticipanteId(entidade.ParticipanteId);

        return entidade.Tipo switch
        {
            TipoDivisaoEntity.PorIgual => new ParticipacaoDespesa.PorIgual(participanteId),
            TipoDivisaoEntity.PorPeso => new ParticipacaoDespesa.PorPeso(participanteId, ExigirPeso(entidade)),
            TipoDivisaoEntity.PorValorFixo => new ParticipacaoDespesa.PorValorFixo(participanteId, ExigirValor(entidade)),
            var naoSuportado => throw new NotSupportedException(
                $"Tipo de divisão não suportado: {naoSuportado}"),
        };
    }

    private static long ExigirPeso(ParticipacaoDespesaEntity entidade) =>
        entidade.Peso ?? throw new InvalidOperationException(
            $"Participação {entidade.Id} persistida como PorPeso sem peso — dado inconsistente no banco.");

    private static Dinheiro ExigirValor(ParticipacaoDespesaEntity entidade) =>
        entidade.ValorCentavos is { } valorCentavos
            ? Dinheiro.EmCentavos(valorCentavos)
            : throw new InvalidOperationException(
                $"Participação {entidade.Id} persistida como PorValorFixo sem valor — dado inconsistente no banco.");
}
