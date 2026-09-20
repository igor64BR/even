using Rateio.Application.Despesas;

namespace Rateio.Application.Grupos;

/// <summary>
/// T28.3: <c>GET /groups/{id}</c> — grupo completo (nome, categoria, participantes, despesas),
/// fechando a lacuna reportada por T22: entrar via link (<c>POST /groups/join/{codigo}</c>, T21) só
/// devolve <c>{grupoId}</c>, sem jeito de o app buscar o resto dos dados do grupo depois. Mesmo
/// padrão de acesso de <see cref="Simplificacao.ObterSimplificacaoDeDividasUseCase"/> (T32) e
/// <see cref="Notificacoes.ObterEventosDeGrupoUseCase"/> (T39.1): valida RNF07 via
/// <see cref="VerificacaoDeAcessoAoGrupo"/> antes de qualquer leitura —
/// <see cref="GrupoNaoEncontradoException"/> (404) / <see cref="AcessoNegadoException"/> (403).
///
/// Reaproveita <see cref="IGrupoRepository.ObterParaEntradaAsync"/> (T21.2) pra nome/categoria/
/// participantes e <see cref="IDespesaRepository.ObterDetalhadasPorGrupoAsync"/> (T28.3) pras
/// despesas, em vez de introduzir uma terceira leitura de grupo com formato próprio — ver
/// <see cref="GrupoCompleto"/> pra por que a composição dos dois já é suficiente.
/// </summary>
public sealed class ObterGrupoUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository)
{
    public async Task<GrupoCompleto> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var grupoParaEntrada = await grupoRepository.ObterParaEntradaAsync(grupoId, cancellationToken)
            ?? throw new GrupoNaoEncontradoException(grupoId);
        var despesas = await despesaRepository.ObterDetalhadasPorGrupoAsync(grupoId, cancellationToken);

        return new GrupoCompleto(grupoParaEntrada, despesas);
    }
}
