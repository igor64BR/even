namespace Rateio.Application.Despesas;

/// <summary>
/// Persistência de uma despesa avulsa num grupo já existente (T23) — distinto de
/// <c>Rateio.Application.Grupos.IGrupoRepository</c>, que persiste o grafo inteiro de um grupo na
/// primeira sincronização (T18). Separar as duas interfaces evita que este caso de uso "do dia a
/// dia" dependa de uma abstração desenhada pro cenário de bulk (Interface Segregation).
/// Implementação concreta (EF Core) em Rateio.Infrastructure.
/// </summary>
public interface IDespesaRepository
{
    /// <summary>
    /// Adiciona <paramref name="despesa"/> a um grupo que já existe no servidor (<paramref name="grupoId"/>
    /// já foi validado por quem chama — este método não revalida existência do grupo nem RNF07).
    /// Não recalcula nem guarda saldo: isso é sob demanda, via T32.
    /// </summary>
    Task AdicionarAsync(Guid grupoId, DespesaParaPersistir despesa, CancellationToken cancellationToken = default);
}
