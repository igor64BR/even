using Rateio.Domain;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Quitacoes;

/// <summary>
/// Mapeamento Domínio→EF de uma quitação (T35), espelhando
/// <c>Rateio.Infrastructure.Despesas.MapeadorDeDespesaEntity</c> do lado da despesa e
/// <see cref="MapeadorDeQuitacaoParaDominio"/> (T32) na direção oposta desta mesma entidade. Não seta
/// <see cref="QuitacaoEntity.GrupoId"/> — quem chama (<see cref="QuitacaoRepository"/>) atribui a FK
/// direto, mesmo padrão do insert avulso de despesa (T23).
/// </summary>
internal static class MapeadorDeQuitacaoEntity
{
    public static QuitacaoEntity Construir(Quitacao quitacao) => new()
    {
        Id = quitacao.Id,
        PagadorId = quitacao.PagadorId.Valor,
        RecebedorId = quitacao.RecebedorId.Valor,
        ValorCentavos = quitacao.Valor.Centavos,
        CriadoEm = DateTimeOffset.UtcNow,
    };
}
