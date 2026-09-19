namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de uma despesa. Esqueleto mínimo (T2.2); T18 acrescentou
/// <see cref="Data"/> (a data de lançamento informada pelo app — distinta de
/// <see cref="CriadoEm"/>, que é o timestamp de quando a linha chegou no servidor) e
/// <see cref="Participacoes"/> (a divisão por participante, RF17/18/19), porque T18 é a primeira
/// vez que o projeto persiste uma <see cref="Rateio.Domain.Despesa"/> completa.
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

    public DateOnly Data { get; set; }

    public DateTimeOffset CriadoEm { get; set; }

    public GrupoEntity? Grupo { get; set; }

    public List<ParticipacaoDespesaEntity> Participacoes { get; set; } = [];
}
