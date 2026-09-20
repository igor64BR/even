namespace Rateio.Application.Notificacoes;

/// <summary>
/// Disparado por <see cref="Despesas.CriarDespesaUseCase"/> (T23) depois de persistir com sucesso
/// (T38.2). Carrega o suficiente pra o app atualizar a UI sem round-trip adicional (RNF10: entrega
/// em ≤2s) — descrição, valor total e quem pagou, não a lista de participações inteira (isso
/// continua vindo de <c>GET /groups/{id}/settlement</c> sob demanda, T32).
/// </summary>
public sealed record EventoDespesaCriada(
    Guid GrupoId,
    Guid DespesaId,
    string Descricao,
    long ValorTotalCentavos,
    Guid PagadorId) : IEventoDeGrupo
{
    public TipoEventoDeGrupo Tipo => TipoEventoDeGrupo.DespesaCriada;
}
