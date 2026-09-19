using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Mapeamento DTO→Domínio de T18.1 — a primeira vez que o projeto converte um payload JSON vindo
/// do app nos tipos ricos de <c>Rateio.Domain</c> (<see cref="Grupo"/>/<see cref="Participante"/>/
/// <see cref="Despesa"/>). Método por passo, nomeado, sem nenhum método fazendo a tradução
/// inteira de uma vez (Object Calisthenics) — e sem burlar a validação de domínio: quem cria o
/// agregado é sempre <see cref="Grupo.Criar"/> (T15), nunca um construtor solto.
/// </summary>
internal static class ConstrutorDeGrupoSincronizado
{
    public static GrupoParaSincronizar Construir(Guid donoUsuarioId, SincronizarGrupoRequest requisicao)
    {
        var participantes = ConstruirParticipantes(requisicao.Participantes);
        var grupo = ConstruirGrupo(requisicao, participantes);
        var despesas = ConstruirDespesas(requisicao.Despesas);

        return new GrupoParaSincronizar(grupo, donoUsuarioId, despesas);
    }

    private static Grupo ConstruirGrupo(SincronizarGrupoRequest requisicao, IReadOnlyList<Participante> participantes) =>
        Grupo.Criar(NomeGrupo.Criar(requisicao.Nome), requisicao.Categoria, participantes);

    private static List<Participante> ConstruirParticipantes(
        IReadOnlyList<ParticipanteSincronizadoRequest> participantes) =>
        participantes.Select(ConstruirParticipante).ToList();

    private static Participante ConstruirParticipante(ParticipanteSincronizadoRequest requisicao)
    {
        var id = new ParticipanteId(requisicao.Id);
        var nome = NomeParticipante.Criar(requisicao.Nome);

        return requisicao.EhConvidado
            ? Participante.Convidado(id, nome)
            : Participante.Autenticado(id, nome);
    }

    private static List<DespesaParaSincronizar> ConstruirDespesas(
        IReadOnlyList<DespesaSincronizadaRequest> despesas) =>
        despesas.Select(ConstruirDespesa).ToList();

    private static DespesaParaSincronizar ConstruirDespesa(DespesaSincronizadaRequest requisicao)
    {
        var despesa = new Despesa(
            requisicao.Id,
            Dinheiro.EmCentavos(requisicao.ValorTotalCentavos),
            new ParticipanteId(requisicao.PagadorId),
            ConstruirParticipacoes(requisicao.TipoDivisao, requisicao.Participacoes));

        return new DespesaParaSincronizar(despesa, requisicao.Descricao, requisicao.Data);
    }

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
