using Rateio.Application.Despesas;
using Rateio.Infrastructure.Persistence;

namespace Rateio.Infrastructure.Despesas;

/// <summary>
/// Implementação de <see cref="IDespesaRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T23). Insere a despesa direto pela FK (<c>DespesaEntity.GrupoId</c>) em vez de carregar o
/// <c>GrupoEntity</c> inteiro (participantes/despesas existentes) só pra anexar mais uma despesa —
/// o grupo já foi confirmado como existente por <c>IGrupoRepository.ObterAcessoAsync</c> antes deste
/// método ser chamado (<see cref="CriarDespesaUseCase"/>).
/// </summary>
public sealed class DespesaRepository(AppDbContext dbContext) : IDespesaRepository
{
    public async Task AdicionarAsync(
        Guid grupoId, DespesaParaPersistir despesa, CancellationToken cancellationToken = default)
    {
        var entidade = MapeadorDeDespesaEntity.Construir(despesa);
        entidade.GrupoId = grupoId;

        dbContext.Despesas.Add(entidade);
        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
