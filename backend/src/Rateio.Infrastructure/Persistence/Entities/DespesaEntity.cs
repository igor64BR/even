namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de uma despesa. Esqueleto mínimo (T2.2) — propriedades apenas o
/// suficiente para existir a tabela; o modelo completo (participações por igual/peso/valor fixo,
/// quitações) é escopo de T15/T23.
///
/// <see cref="ValorTotalCentavos"/> guarda o mesmo valor bruto que
/// <see cref="Rateio.Domain.Dinheiro.Centavos"/> expõe — a coluna nunca vira <c>decimal</c> cru;
/// isso preserva a reconstrução exata de <see cref="Rateio.Domain.Dinheiro"/> a partir do dado
/// salvo (ver orientação da task T2 e algorithm-spec.md). <see cref="PagadorId"/> corresponde ao
/// <see cref="Rateio.Domain.ParticipanteId.Valor"/> do pagador.
/// </summary>
public class DespesaEntity
{
    public Guid Id { get; set; }

    public Guid GrupoId { get; set; }

    public Guid PagadorId { get; set; }

    public long ValorTotalCentavos { get; set; }

    public string Descricao { get; set; } = string.Empty;

    public DateTimeOffset CriadoEm { get; set; }

    public GrupoEntity? Grupo { get; set; }
}
