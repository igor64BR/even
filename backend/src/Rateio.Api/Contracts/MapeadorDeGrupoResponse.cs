using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Api.Contracts;

/// <summary>
/// Mapeamento Domínio/Application→Response de <see cref="GrupoCompleto"/> pra
/// <see cref="ObterGrupoResponse"/> (T28.3). Espelha, na direção oposta, a mesma tradução por
/// subtipo de <see cref="ParticipacaoDespesa"/> que <c>Rateio.Infrastructure.Despesas.MapeadorDeDespesaEntity</c>
/// já faz Domínio→EF — método por passo, nomeado, sem nenhum fazendo a tradução inteira de uma vez
/// (mesmo Object Calisthenics que os outros mapeadores do projeto seguem).
/// </summary>
internal static class MapeadorDeGrupoResponse
{
    public static ObterGrupoResponse Construir(GrupoCompleto grupoCompleto) => new(
        grupoCompleto.Grupo.Nome.Valor,
        grupoCompleto.Grupo.Categoria,
        ConstruirParticipantes(grupoCompleto.Grupo.Participantes),
        ConstruirDespesas(grupoCompleto.Despesas));

    private static List<ParticipanteResponse> ConstruirParticipantes(IReadOnlyList<Participante> participantes) =>
        participantes
            .Select(participante => new ParticipanteResponse(
                participante.Id.Valor, participante.Nome.Valor, participante.EhConvidado))
            .ToList();

    private static List<DespesaDetalhadaResponse> ConstruirDespesas(IReadOnlyList<DespesaParaPersistir> despesas) =>
        despesas.Select(ConstruirDespesa).ToList();

    private static DespesaDetalhadaResponse ConstruirDespesa(DespesaParaPersistir despesaParaPersistir)
    {
        var despesa = despesaParaPersistir.Despesa;

        return new DespesaDetalhadaResponse(
            despesa.Id,
            despesaParaPersistir.Descricao,
            despesa.ValorTotal.Centavos,
            despesa.PagadorId.Valor,
            despesaParaPersistir.Data,
            ObterTipoDivisao(despesa.Participacoes),
            ConstruirParticipacoes(despesa.Participacoes));
    }

    /// <summary>
    /// Todas as participações de uma mesma despesa são do mesmo subtipo concreto de
    /// <see cref="ParticipacaoDespesa"/> (invariante garantida na borda de entrada — ver o
    /// comentário XML daquele tipo), então o tipo da primeira já identifica o da despesa inteira.
    /// </summary>
    private static TipoDivisaoRequest ObterTipoDivisao(IReadOnlyList<ParticipacaoDespesa> participacoes) =>
        participacoes[0] switch
        {
            ParticipacaoDespesa.PorIgual => TipoDivisaoRequest.PorIgual,
            ParticipacaoDespesa.PorPeso => TipoDivisaoRequest.PorPeso,
            ParticipacaoDespesa.PorValorFixo => TipoDivisaoRequest.PorValorFixo,
            var naoSuportada => throw new NotSupportedException(
                $"Tipo de participação não suportado: {naoSuportada.GetType().Name}"),
        };

    private static List<ParticipacaoResponse> ConstruirParticipacoes(IReadOnlyList<ParticipacaoDespesa> participacoes) =>
        participacoes.Select(ConstruirParticipacao).ToList();

    private static ParticipacaoResponse ConstruirParticipacao(ParticipacaoDespesa participacao) =>
        participacao switch
        {
            ParticipacaoDespesa.PorIgual porIgual =>
                new ParticipacaoResponse(porIgual.ParticipanteId.Valor, Peso: null, ValorCentavos: null),
            ParticipacaoDespesa.PorPeso porPeso =>
                new ParticipacaoResponse(porPeso.ParticipanteId.Valor, porPeso.Peso, ValorCentavos: null),
            ParticipacaoDespesa.PorValorFixo porValorFixo =>
                new ParticipacaoResponse(porValorFixo.ParticipanteId.Valor, Peso: null, porValorFixo.Valor.Centavos),
            var naoSuportada => throw new NotSupportedException(
                $"Tipo de participação não suportado: {naoSuportada.GetType().Name}"),
        };
}
