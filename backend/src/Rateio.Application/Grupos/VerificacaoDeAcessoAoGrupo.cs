namespace Rateio.Application.Grupos;

/// <summary>
/// RNF07 centralizado: "o grupo existe, e o usuário autenticado é o dono dele" — checado antes de
/// qualquer operação em nome de um usuário sobre um grupo específico (<see cref="GrupoNaoEncontradoException"/>
/// se o grupo não existe, <see cref="AcessoNegadoException"/> se existe mas não pertence ao
/// usuário). Extraído de <c>CriarDespesaUseCase</c> (T23) para ser reaproveitado por
/// <c>Rateio.Application.Simplificacao.ObterSimplificacaoDeDividasUseCase</c> (T32) — evita que a
/// mesma checagem de acesso seja copiada uma terceira vez por qualquer futuro caso de uso "por
/// grupo" (T35 incluso).
/// </summary>
internal static class VerificacaoDeAcessoAoGrupo
{
    public static async Task<AcessoAoGrupo> ExigirAsync(
        IGrupoRepository grupoRepository,
        Guid grupoId,
        Guid usuarioAutenticadoId,
        CancellationToken cancellationToken)
    {
        var acesso = await grupoRepository.ObterAcessoAsync(grupoId, cancellationToken)
            ?? throw new GrupoNaoEncontradoException(grupoId);

        if (!acesso.PertenceA(usuarioAutenticadoId))
        {
            throw new AcessoNegadoException(usuarioAutenticadoId, acesso.GrupoId);
        }

        return acesso;
    }
}
