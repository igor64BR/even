using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Despesas;

/// <summary>
/// Mapeamento DTO→Domínio de uma despesa, extraído de
/// <c>Rateio.Application.Grupos.ConstrutorDeGrupoSincronizado</c> (T18.1) para ser reaproveitado por
/// T23 (<c>POST /groups/{id}/expenses</c>, uma despesa avulsa) sem duplicar a lógica que já existia
/// pro caso de sincronização em massa. Continua reaproveitando os DTOs de despesa de
/// <see cref="Rateio.Application.Grupos"/> (<see cref="DespesaSincronizadaRequest"/> e afins) em vez
/// de criar um segundo conjunto de tipos com o mesmo formato — o payload de "uma despesa nova" é
/// estruturalmente idêntico ao de "uma despesa dentro do bulk de sincronização".
///
/// Método por passo, nomeado, sem nenhum método fazendo a tradução inteira de uma vez (Object
/// Calisthenics) — e sem burlar validação de domínio: quem valida os subtipos de
/// <see cref="ParticipacaoDespesa"/> continua sendo os construtores de T31.
/// </summary>
internal static class MapeadorDeDespesa
{
    public static Despesa Construir(DespesaSincronizadaRequest requisicao) =>
        new(
            requisicao.Id,
            Dinheiro.EmCentavos(requisicao.ValorTotalCentavos),
            new ParticipanteId(requisicao.PagadorId),
            ConstruirParticipacoes(requisicao.TipoDivisao, requisicao.Participacoes));

    private static List<ParticipacaoDespesa> ConstruirParticipacoes(
        TipoDivisaoRequest tipo, IReadOnlyList<ParticipacaoSincronizadaRequest> participacoes) =>
        participacoes.Select(participacao => ConstruirParticipacao(tipo, participacao)).ToList();

    private static ParticipacaoDespesa ConstruirParticipacao(
        TipoDivisaoRequest tipo, ParticipacaoSincronizadaRequest requisicao)
    {
        var participanteId = new ParticipanteId(requisicao.ParticipanteId);

        return tipo switch
        {
            TipoDivisaoRequest.PorIgual => new ParticipacaoDespesa.PorIgual(participanteId),
            TipoDivisaoRequest.PorPeso => new ParticipacaoDespesa.PorPeso(participanteId, ExigirPeso(requisicao)),
            TipoDivisaoRequest.PorValorFixo => new ParticipacaoDespesa.PorValorFixo(participanteId, ExigirValor(requisicao)),
            _ => throw new ArgumentOutOfRangeException(nameof(tipo), tipo, "Tipo de divisão não suportado."),
        };
    }

    private static long ExigirPeso(ParticipacaoSincronizadaRequest requisicao) =>
        requisicao.Peso ?? throw new ArgumentException(
            "Peso é obrigatório numa participação de despesa dividida por peso.", nameof(requisicao));

    private static Dinheiro ExigirValor(ParticipacaoSincronizadaRequest requisicao) =>
        requisicao.ValorCentavos is { } valorCentavos
            ? Dinheiro.EmCentavos(valorCentavos)
            : throw new ArgumentException(
                "Valor é obrigatório numa participação de despesa dividida por valor fixo.", nameof(requisicao));
}
