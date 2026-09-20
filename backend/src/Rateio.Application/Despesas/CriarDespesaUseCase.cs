using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;

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
/// saldo, isso é sob demanda via T32. (5) T38.2: com a despesa já persistida com sucesso, notifica
/// <see cref="INotificadorDeEventoDeGrupo"/> (RF35/RF36) — depende só da abstração de Application,
/// nunca de SignalR diretamente (Dependency Inversion).
///
/// <paramref name="usuarioAutenticadoId"/> só é usado pra checar acesso, nunca é gravado como parte
/// da despesa — igual à garantia estrutural de T18 pro dono do grupo.
/// </summary>
public sealed class CriarDespesaUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository,
    INotificadorDeEventoDeGrupo notificadorDeEventoDeGrupo)
{
    public async Task<Guid> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        DespesaSincronizadaRequest requisicao,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var despesa = MapeadorDeDespesa.Construir(requisicao);
        var despesaParaPersistir = new DespesaParaPersistir(despesa, requisicao.Descricao, requisicao.Data);

        await despesaRepository.AdicionarAsync(grupoId, despesaParaPersistir, cancellationToken);

        var evento = new EventoDespesaCriada(
            grupoId,
            despesa.Id,
            despesaParaPersistir.Descricao,
            despesa.ValorTotal.Centavos,
            despesa.PagadorId.Valor);
        await notificadorDeEventoDeGrupo.NotificarAsync(evento, cancellationToken);

        return despesa.Id;
    }
}
