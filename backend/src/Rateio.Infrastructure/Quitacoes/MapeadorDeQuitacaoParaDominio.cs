using Rateio.Domain;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Quitacoes;

/// <summary>
/// Mapeamento EF→Domínio de uma quitação (T32) — espelha <see cref="Despesas.MapeadorDeDespesaParaDominio"/>
/// do lado da quitação. Sem subtipos/discriminador para reconstruir (diferente de
/// <see cref="Domain.ParticipacaoDespesa"/>), a tradução é direta o bastante para não precisar de
/// métodos privados por passo.
/// </summary>
internal static class MapeadorDeQuitacaoParaDominio
{
    public static Quitacao Construir(QuitacaoEntity entidade) => new(
        entidade.Id,
        new ParticipanteId(entidade.PagadorId),
        new ParticipanteId(entidade.RecebedorId),
        Dinheiro.EmCentavos(entidade.ValorCentavos));
}
