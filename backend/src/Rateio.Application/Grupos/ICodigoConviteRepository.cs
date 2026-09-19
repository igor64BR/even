namespace Rateio.Application.Grupos;

/// <summary>
/// Persistência dos códigos de convite (T21.1/T21.2). Interface separada de
/// <see cref="IGrupoRepository"/> (Interface Segregation, mesmo racional de
/// <c>Rateio.Application.Despesas.IDespesaRepository</c>): quem gera/resolve código de convite não
/// precisa conhecer o resto da persistência de grupo. Implementação concreta (EF Core) em
/// Rateio.Infrastructure.
/// </summary>
public interface ICodigoConviteRepository
{
    /// <summary>
    /// Persiste <paramref name="codigo"/> como o único código ativo do grupo
    /// (<see cref="CodigoConvite.GrupoId"/>) — upsert por grupo: qualquer código anterior desse
    /// mesmo grupo deixa de ser resolvível por <see cref="ObterPorCodigoAsync"/> depois desta
    /// chamada (ver <see cref="CodigoConvite"/> para a justificativa de "um código ativo por
    /// grupo").
    /// </summary>
    Task SalvarAsync(CodigoConvite codigo, CancellationToken cancellationToken = default);

    /// <summary>
    /// Resolve um código pro convite correspondente, ou <c>null</c> se o código nunca existiu ou já
    /// foi substituído por um mais novo (ver <see cref="SalvarAsync"/>). Não checa expiração — quem
    /// chama decide isso via <see cref="CodigoConvite.EstaValido"/>, porque "código não encontrado"
    /// e "código expirado" são duas causas distintas do mesmo 404 HTTP, mas merecem mensagens de
    /// diagnóstico diferentes.
    /// </summary>
    Task<CodigoConvite?> ObterPorCodigoAsync(string codigo, CancellationToken cancellationToken = default);
}
