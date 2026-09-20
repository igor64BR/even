namespace Rateio.Application.Quitacoes;

/// <summary>
/// T39.1: projeção de leitura para o fallback de pull (<c>GET /groups/{id}/events</c>,
/// <see cref="Notificacoes.ObterEventosDeGrupoUseCase"/>) — mesmo papel de
/// <see cref="Despesas.DespesaOcorrida"/>, só que para quitações: o suficiente pra montar um
/// <see cref="Notificacoes.EventoDividaQuitada"/> mais <see cref="CriadoEm"/> (timestamp do
/// servidor), usado tanto para filtrar "desde" quanto para ordenar cronologicamente junto com
/// despesas no use case. Distinta de <see cref="Rateio.Domain.Quitacao"/> (T31: o motor de
/// simplificação não usa timestamp).
/// </summary>
public sealed record QuitacaoOcorrida(
    Guid Id,
    Guid PagadorId,
    Guid RecebedorId,
    long ValorCentavos,
    DateTimeOffset CriadoEm);
