using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;

namespace Rateio.Application.Quitacoes;

/// <summary>
/// T35.1: <c>POST /groups/{id}/settlements</c> — registra que uma das transações sugeridas por
/// <c>GET /groups/{id}/settlement</c> (T32) foi paga (RF31/RF33). Mesmo padrão de
/// <c>CriarDespesaUseCase</c> (T23): (1) valida acesso ao grupo via
/// <see cref="VerificacaoDeAcessoAoGrupo"/> — <see cref="GrupoNaoEncontradoException"/> se o grupo
/// não existe (404), <see cref="AcessoNegadoException"/> se existe mas o usuário autenticado não tem
/// acesso a ele (403, RNF07); só então (2) o payload vira <see cref="Domain.Quitacao"/> de domínio
/// (T31) via <see cref="MapeadorDeQuitacao"/> e (3) persiste via <see cref="IQuitacaoRepository"/>.
/// (4) T38.3: com a quitação já persistida com sucesso, notifica
/// <see cref="INotificadorDeEventoDeGrupo"/> (RF35/RF36) — mesma abstração de Application usada por
/// <c>CriarDespesaUseCase</c>, nunca SignalR direto.
///
/// Não recalcula nem guarda saldo (T35.2): a próxima chamada a <c>GET /groups/{id}/settlement</c> já
/// recomputa sob demanda a partir do histórico vigente de despesas e quitações — mesma garantia que
/// <c>ObterSimplificacaoDeDividasUseCase</c> (T32) já oferece.
///
/// <paramref name="usuarioAutenticadoId"/> só é usado pra checar acesso, nunca é gravado como parte
/// da quitação — igual à garantia estrutural de <c>CriarDespesaUseCase</c>.
/// </summary>
public sealed class RegistrarQuitacaoUseCase(
    IGrupoRepository grupoRepository,
    IQuitacaoRepository quitacaoRepository,
    INotificadorDeEventoDeGrupo notificadorDeEventoDeGrupo)
{
    public async Task<Guid> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        RegistrarQuitacaoRequest requisicao,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var quitacao = MapeadorDeQuitacao.Construir(requisicao);

        await quitacaoRepository.AdicionarAsync(grupoId, quitacao, cancellationToken);

        var evento = new EventoDividaQuitada(
            grupoId,
            quitacao.Id,
            quitacao.PagadorId.Valor,
            quitacao.RecebedorId.Valor,
            quitacao.Valor.Centavos);
        await notificadorDeEventoDeGrupo.NotificarAsync(evento, cancellationToken);

        return quitacao.Id;
    }
}
