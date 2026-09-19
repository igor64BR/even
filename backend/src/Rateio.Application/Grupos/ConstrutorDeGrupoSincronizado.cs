using Rateio.Application.Despesas;
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

    private static List<DespesaParaPersistir> ConstruirDespesas(
        IReadOnlyList<DespesaSincronizadaRequest> despesas) =>
        despesas.Select(ConstruirDespesa).ToList();

    // T23 extraiu a construção de Despesa/ParticipacaoDespesa em si pra
    // Rateio.Application.Despesas.MapeadorDeDespesa — reaproveitada aqui e pelo novo caso de uso de
    // despesa avulsa, em vez de duplicada.
    private static DespesaParaPersistir ConstruirDespesa(DespesaSincronizadaRequest requisicao) =>
        new(MapeadorDeDespesa.Construir(requisicao), requisicao.Descricao, requisicao.Data);
}
