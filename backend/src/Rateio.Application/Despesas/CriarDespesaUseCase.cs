using Rateio.Application.Grupos;

namespace Rateio.Application.Despesas;

/// <summary>
/// T23.2: adiciona UMA despesa nova a um grupo que já está sincronizado (RF.., operação do dia a
/// dia) — diferente de <see cref="SincronizarGrupoUseCase"/> (T18), que sobe o estado local inteiro
/// de um grupo na primeira sincronização.
///
/// Ordem de validação (mesma da task): (1) o grupo existe — senão <see cref="GrupoNaoEncontradoException"/>
/// (o controller mapeia pra 404); (2) o usuário autenticado tem acesso ao grupo, RNF07 — senão
/// <see cref="AcessoNegadoException"/> (403); só then (3) o payload vira <see cref="Domain.Despesa"/>
/// de domínio via <see cref="MapeadorDeDespesa"/> (o mesmo mapeamento DTO→Domínio que T18 escreveu
/// pra sincronização, extraído pra <c>Rateio.Application.Despesas</c> porque os dois casos de uso
/// precisam dele) e (4) persiste via <see cref="IDespesaRepository"/> — sem recalcular/guardar
/// saldo, isso é sob demanda via T32.
///
/// <paramref name="usuarioAutenticadoId"/> só é usado pra checar acesso, nunca é gravado como parte
/// da despesa — igual à garantia estrutural de T18 pro dono do grupo.
/// </summary>
public sealed class CriarDespesaUseCase(IGrupoRepository grupoRepository, IDespesaRepository despesaRepository)
{
    public async Task<Guid> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        DespesaSincronizadaRequest requisicao,
        CancellationToken cancellationToken = default)
    {
        var acesso = await ObterAcessoOuFalhar(grupoId, cancellationToken);
        ExigirAcesso(acesso, usuarioAutenticadoId);

        var despesa = MapeadorDeDespesa.Construir(requisicao);
        var despesaParaPersistir = new DespesaParaPersistir(despesa, requisicao.Descricao, requisicao.Data);

        await despesaRepository.AdicionarAsync(grupoId, despesaParaPersistir, cancellationToken);

        return despesa.Id;
    }

    private async Task<AcessoAoGrupo> ObterAcessoOuFalhar(Guid grupoId, CancellationToken cancellationToken) =>
        await grupoRepository.ObterAcessoAsync(grupoId, cancellationToken)
            ?? throw new GrupoNaoEncontradoException(grupoId);

    private static void ExigirAcesso(AcessoAoGrupo acesso, Guid usuarioAutenticadoId)
    {
        if (!acesso.PertenceA(usuarioAutenticadoId))
        {
            throw new AcessoNegadoException(usuarioAutenticadoId, acesso.GrupoId);
        }
    }
}
