namespace Rateio.Application.Grupos;

/// <summary>
/// T21.1: gera o código de convite de um grupo sincronizado. Reaproveita
/// <see cref="IGrupoRepository.ObterAcessoAsync"/> (o mesmo usado por
/// <c>Rateio.Application.Despesas.CriarDespesaUseCase</c>, T23) pra validar existência do grupo e
/// dono — <see cref="AcessoAoGrupo.PertenceA"/> hoje É a checagem de dono (ver o comentário daquele
/// tipo), então "só o dono" (RNF07/T21) é exatamente a mesma regra, sem precisar de um conceito
/// novo.
///
/// Ordem de validação (mesmo padrão de T23): (1) grupo existe, senão
/// <see cref="GrupoNaoEncontradoException"/> (404); (2) usuário autenticado é o dono, senão
/// <see cref="AcessoNegadoException"/> (403 — "gerar código só o dono", diferente de "entrar via
/// código", que T21.2 abre pra qualquer autenticado).
/// </summary>
public sealed class GerarCodigoConviteUseCase(
    IGrupoRepository grupoRepository,
    ICodigoConviteRepository codigoConviteRepository,
    TimeProvider relogio)
{
    private static readonly TimeSpan ValidadeDoCodigo = TimeSpan.FromDays(7);

    public async Task<CodigoConvite> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        CancellationToken cancellationToken = default)
    {
        var acesso = await ObterAcessoOuFalhar(grupoId, cancellationToken);
        ExigirDono(acesso, usuarioAutenticadoId);

        var codigo = GerarCodigo(grupoId);

        await codigoConviteRepository.SalvarAsync(codigo, cancellationToken);

        return codigo;
    }

    private async Task<AcessoAoGrupo> ObterAcessoOuFalhar(Guid grupoId, CancellationToken cancellationToken) =>
        await grupoRepository.ObterAcessoAsync(grupoId, cancellationToken)
            ?? throw new GrupoNaoEncontradoException(grupoId);

    private static void ExigirDono(AcessoAoGrupo acesso, Guid usuarioAutenticadoId)
    {
        if (!acesso.PertenceA(usuarioAutenticadoId))
        {
            throw new AcessoNegadoException(usuarioAutenticadoId, acesso.GrupoId);
        }
    }

    private CodigoConvite GerarCodigo(Guid grupoId)
    {
        var agora = relogio.GetUtcNow();

        return new CodigoConvite(GeradorDeCodigoConvite.Gerar(), grupoId, agora, agora + ValidadeDoCodigo);
    }
}
