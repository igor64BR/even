namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência da parte de um participante numa despesa (T18). Não existe um tipo
/// de domínio equivalente a <see cref="Tipo"/> — <c>Rateio.Domain.ParticipacaoDespesa</c> expressa
/// deliberadamente o tipo de divisão via subtipo concreto (<c>PorIgual</c>/<c>PorPeso</c>/
/// <c>PorValorFixo</c>) em vez de um enum solto, pra não ter duas fontes de verdade divergentes no
/// domínio (ver o comentário XML daquele tipo). Aqui é diferente: isso é só a coluna
/// discriminadora que uma tabela relacional precisa pra saber qual subtipo reconstruir na leitura
/// — interno da Infrastructure, nunca vaza pro domínio.
/// </summary>
public class ParticipacaoDespesaEntity
{
    public Guid Id { get; set; }

    public Guid DespesaId { get; set; }

    public Guid ParticipanteId { get; set; }

    public TipoDivisaoEntity Tipo { get; set; }

    /// <summary>Só preenchido quando <see cref="Tipo"/> é <see cref="TipoDivisaoEntity.PorPeso"/>.</summary>
    public long? Peso { get; set; }

    /// <summary>Só preenchido quando <see cref="Tipo"/> é <see cref="TipoDivisaoEntity.PorValorFixo"/>.</summary>
    public long? ValorCentavos { get; set; }

    public DespesaEntity? Despesa { get; set; }
}

/// <summary>Discriminador de persistência de <see cref="ParticipacaoDespesaEntity.Tipo"/> — ver o
/// comentário da classe para por que isso não existe do lado do domínio.</summary>
public enum TipoDivisaoEntity
{
    PorIgual,
    PorPeso,
    PorValorFixo,
}
