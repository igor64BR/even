namespace Rateio.Application.Despesas;

/// <summary>
/// T39.1: projeção de leitura para o fallback de pull (<c>GET /groups/{id}/events</c>,
/// <see cref="Notificacoes.ObterEventosDeGrupoUseCase"/>) — carrega só o que é preciso para montar
/// um <see cref="Notificacoes.EventoDespesaCriada"/> mais <see cref="CriadoEm"/>, o timestamp de
/// quando a despesa foi persistida no servidor. Distinta de <see cref="Rateio.Domain.Despesa"/>
/// (T31: o motor de simplificação não usa descrição nem timestamp) e de
/// <see cref="DespesaParaPersistir"/> (que carrega <c>Data</c>, a data de lançamento informada pelo
/// app — granularidade de dia, não serve como cursor de sincronização). <see cref="CriadoEm"/> é o
/// mesmo campo usado tanto para filtrar "desde" quanto para ordenar cronologicamente junto com
/// <see cref="Quitacoes.QuitacaoOcorrida"/> no use case.
/// </summary>
public sealed record DespesaOcorrida(
    Guid Id,
    string Descricao,
    long ValorTotalCentavos,
    Guid PagadorId,
    DateTimeOffset CriadoEm);
