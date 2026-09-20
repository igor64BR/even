using Rateio.Application.Grupos;

namespace Rateio.Application.Despesas;

/// <summary>
/// T28.1: <c>PUT /groups/{id}/expenses/{expenseId}</c> — edita uma despesa já persistida (RF20).
/// Mesmo padrão de acesso de <see cref="CriarDespesaUseCase"/> (T23): (1) RNF07 via
/// <see cref="VerificacaoDeAcessoAoGrupo"/> — <see cref="GrupoNaoEncontradoException"/> se o grupo
/// não existe (404), <see cref="AcessoNegadoException"/> se existe mas o usuário autenticado não
/// tem acesso a ele (403); só então (2) o payload vira uma <see cref="Domain.Despesa"/> de domínio
/// inteira via <see cref="MapeadorDeDespesa"/> — a mesma validação de invariante que
/// <see cref="CriarDespesaUseCase"/> aplica, nunca um update parcial de campo solto — e (3) persiste
/// via <see cref="IDespesaRepository.AtualizarAsync"/>, que devolve <c>false</c> (sem lançar) quando
/// a despesa não existe nesse grupo — vira <see cref="DespesaNaoEncontradaException"/> (404) aqui.
///
/// <paramref name="despesaId"/> vem sempre da rota, nunca do corpo da requisição — o mesmo cuidado
/// estrutural de RNF07 (nunca confiar em identidade vinda do payload): o <c>Id</c> de
/// <paramref name="requisicao"/> é substituído pelo da rota antes de qualquer mapeamento, então não
/// há como o corpo da requisição editar uma despesa diferente da que a URL aponta.
///
/// Não recalcula nem guarda saldo (mesmo raciocínio de T35.2): a próxima chamada a
/// <c>GET /groups/{id}/settlement</c> já recomputa sob demanda a partir do histórico vigente.
/// </summary>
public sealed class EditarDespesaUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository)
{
    public async Task ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        Guid despesaId,
        DespesaSincronizadaRequest requisicao,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var requisicaoComIdDaRota = requisicao with { Id = despesaId };
        var despesa = MapeadorDeDespesa.Construir(requisicaoComIdDaRota);
        var despesaParaPersistir = new DespesaParaPersistir(
            despesa, requisicaoComIdDaRota.Descricao, requisicaoComIdDaRota.Data);

        var atualizada = await despesaRepository.AtualizarAsync(grupoId, despesaParaPersistir, cancellationToken);
        if (!atualizada)
        {
            throw new DespesaNaoEncontradaException(grupoId, despesaId);
        }
    }
}
