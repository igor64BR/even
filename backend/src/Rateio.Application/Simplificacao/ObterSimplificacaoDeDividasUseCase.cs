using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Quitacoes;
using Rateio.Domain;

namespace Rateio.Application.Simplificacao;

/// <summary>
/// T32.1: <c>GET /groups/{id}/settlement</c> — carrega o histórico vigente de despesas e
/// quitações de um grupo já sincronizado e delega ao motor de simplificação (T31,
/// <see cref="IMotorDeSimplificacaoDeDividas"/>) o cálculo do saldo e da lista de transações
/// sugeridas. Este caso de uso só orquestra: acesso (RNF07), leitura via os repositórios e a
/// chamada às duas funções do motor, nessa ordem — nenhuma regra de saldo/settlement mora aqui,
/// isso é 100% responsabilidade de T31 (constituição, princípio 4).
///
/// O resultado nunca é persistido: é sempre recomputado do zero a partir do histórico vigente
/// (mesma garantia que <c>algorithm-spec.md</c> exige de <c>computeBalances</c>), então não existe
/// risco de saldo desatualizado ficar "preso" em cache.
/// </summary>
public sealed class ObterSimplificacaoDeDividasUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository,
    IQuitacaoRepository quitacaoRepository,
    IMotorDeSimplificacaoDeDividas motor)
{
    public async Task<IReadOnlyList<Transacao>> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var despesas = await despesaRepository.ObterPorGrupoAsync(grupoId, cancellationToken);
        var quitacoes = await quitacaoRepository.ObterPorGrupoAsync(grupoId, cancellationToken);

        var saldos = motor.ComputeBalances(despesas, quitacoes);
        return motor.ComputeSettlement(saldos);
    }
}
