using Rateio.Application.Grupos;

namespace Rateio.Application.Despesas;

/// <summary>
/// T28.2: <c>DELETE /groups/{id}/expenses/{expenseId}</c> — exclui uma despesa já persistida
/// (RF21). Mesmo padrão de acesso de <see cref="CriarDespesaUseCase"/>/<see cref="EditarDespesaUseCase"/>:
/// RNF07 via <see cref="VerificacaoDeAcessoAoGrupo"/> antes de qualquer coisa —
/// <see cref="GrupoNaoEncontradoException"/> (404) / <see cref="AcessoNegadoException"/> (403) — só
/// então remove via <see cref="IDespesaRepository.RemoverAsync"/>, que devolve <c>false</c> (sem
/// lançar) quando a despesa não existe nesse grupo, virando <see cref="DespesaNaoEncontradaException"/>
/// (404) aqui.
///
/// Não recalcula nem guarda saldo — mesmo raciocínio de <see cref="EditarDespesaUseCase"/>: a
/// próxima chamada a <c>GET /groups/{id}/settlement</c> já recomputa sob demanda.
/// </summary>
public sealed class ExcluirDespesaUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository)
{
    public async Task ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        Guid despesaId,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var removida = await despesaRepository.RemoverAsync(grupoId, despesaId, cancellationToken);
        if (!removida)
        {
            throw new DespesaNaoEncontradaException(grupoId, despesaId);
        }
    }
}
