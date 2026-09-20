namespace Rateio.Application.Despesas;

/// <summary>
/// T28.1/T28.2: a despesa referenciada (<c>PUT</c>/<c>DELETE /groups/{id}/expenses/{expenseId}</c>)
/// não existe nesse grupo — grupo pode existir (RNF07 já validado por
/// <see cref="Grupos.VerificacaoDeAcessoAoGrupo"/> antes desta checagem), só a despesa que não.
/// Nunca vira 500 — o controller mapeia isso pra 404, mesmo padrão de
/// <see cref="Grupos.GrupoNaoEncontradoException"/>.
/// </summary>
public sealed class DespesaNaoEncontradaException(Guid grupoId, Guid despesaId)
    : Exception($"Despesa {despesaId} não encontrada no grupo {grupoId}.")
{
    public Guid GrupoId { get; } = grupoId;

    public Guid DespesaId { get; } = despesaId;
}
