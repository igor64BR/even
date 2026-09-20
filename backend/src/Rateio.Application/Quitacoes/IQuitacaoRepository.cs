using Rateio.Domain;

namespace Rateio.Application.Quitacoes;

/// <summary>
/// Persistência de quitações (RF31/RF33) — mesmo padrão de <c>Rateio.Application.Despesas.IDespesaRepository</c>:
/// abstração própria (Interface Segregation) em vez de pendurar leitura/escrita de quitação numa
/// interface desenhada pra outra entidade. T32 introduziu aqui o método de leitura, que é o que
/// <c>ObterSimplificacaoDeDividasUseCase</c> precisa para alimentar o motor de simplificação
/// (T31) com o histórico vigente de quitações de um grupo. T35 acrescenta o método de escrita —
/// registrar que uma transação sugerida foi paga — sem quebrar quem já depende da leitura.
/// Implementação concreta (EF Core) em Rateio.Infrastructure.
/// </summary>
public interface IQuitacaoRepository
{
    /// <summary>
    /// Todas as quitações vigentes de um grupo, já como tipo de domínio. <paramref name="grupoId"/>
    /// já foi validado por quem chama (RNF07) — este método não revalida existência do grupo nem
    /// acesso.
    /// </summary>
    Task<IReadOnlyList<Quitacao>> ObterPorGrupoAsync(Guid grupoId, CancellationToken cancellationToken = default);

    /// <summary>
    /// T35: persiste <paramref name="quitacao"/> como pertencente ao grupo <paramref name="grupoId"/>
    /// (já validado por quem chama — este método não revalida existência do grupo nem RNF07). Mesmo
    /// contrato de <c>IDespesaRepository.AdicionarAsync</c>: não recalcula nem guarda saldo, isso é
    /// sob demanda via T32 na próxima leitura de <c>ObterPorGrupoAsync</c>.
    /// </summary>
    Task AdicionarAsync(Guid grupoId, Quitacao quitacao, CancellationToken cancellationToken = default);
}
