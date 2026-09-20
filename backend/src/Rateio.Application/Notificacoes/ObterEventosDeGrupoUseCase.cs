using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Quitacoes;

namespace Rateio.Application.Notificacoes;

/// <summary>
/// T39.1: <c>GET /groups/{id}/events?desde={timestampIso8601}</c> — fallback de pull
/// (constitution.md princípio 3: sem push de terceiros, o app só recebe evento em tempo real
/// enquanto a conexão SignalR (T38) está ativa; ao reconectar, precisa recuperar o que perdeu).
/// Não existe uma tabela de eventos: despesas e quitações já têm timestamp de servidor
/// (<c>CriadoEm</c>) suficiente pra reconstruir "o que aconteceu nesse grupo desde X" — este use
/// case só consulta as duas, cada uma via sua própria projeção de leitura
/// (<see cref="DespesaOcorrida"/>/<see cref="QuitacaoOcorrida"/>, expostas pelos repositórios já
/// existentes), monta o mesmo tipo de evento que T38 já usa em tempo real
/// (<see cref="EventoDespesaCriada"/>/<see cref="EventoDividaQuitada"/>) e devolve tudo
/// intercalado, ordenado por <c>CriadoEm</c> crescente.
///
/// Mesmo padrão de acesso de <c>Simplificacao.ObterSimplificacaoDeDividasUseCase</c> (T32) e
/// <c>Quitacoes.RegistrarQuitacaoUseCase</c> (T35): valida RNF07 via
/// <see cref="VerificacaoDeAcessoAoGrupo"/> antes de qualquer leitura —
/// <see cref="GrupoNaoEncontradoException"/> se o grupo não existe (404, mapeado no controller),
/// <see cref="AcessoNegadoException"/> se existe mas o usuário autenticado não tem acesso a ele
/// (403).
/// </summary>
public sealed class ObterEventosDeGrupoUseCase(
    IGrupoRepository grupoRepository,
    IDespesaRepository despesaRepository,
    IQuitacaoRepository quitacaoRepository)
{
    public async Task<IReadOnlyList<IEventoDeGrupo>> ExecutarAsync(
        Guid usuarioAutenticadoId,
        Guid grupoId,
        DateTimeOffset desde,
        CancellationToken cancellationToken = default)
    {
        await VerificacaoDeAcessoAoGrupo.ExigirAsync(grupoRepository, grupoId, usuarioAutenticadoId, cancellationToken);

        var despesas = await despesaRepository.ObterOcorridasDesdeAsync(grupoId, desde, cancellationToken);
        var quitacoes = await quitacaoRepository.ObterOcorridasDesdeAsync(grupoId, desde, cancellationToken);

        var eventosDeDespesa = despesas.Select(despesa => new
        {
            Evento = (IEventoDeGrupo)new EventoDespesaCriada(
                grupoId, despesa.Id, despesa.Descricao, despesa.ValorTotalCentavos, despesa.PagadorId),
            despesa.CriadoEm,
        });

        var eventosDeQuitacao = quitacoes.Select(quitacao => new
        {
            Evento = (IEventoDeGrupo)new EventoDividaQuitada(
                grupoId, quitacao.Id, quitacao.PagadorId, quitacao.RecebedorId, quitacao.ValorCentavos),
            quitacao.CriadoEm,
        });

        return eventosDeDespesa
            .Concat(eventosDeQuitacao)
            .OrderBy(item => item.CriadoEm)
            .Select(item => item.Evento)
            .ToList();
    }
}
