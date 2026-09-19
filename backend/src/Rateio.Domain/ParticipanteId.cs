namespace Rateio.Domain;

/// <summary>
/// Identificador estável de um participante. Envolve o valor bruto (<see cref="Guid"/>) em vez
/// de deixá-lo circular cru pelo domínio (Object Calisthenics: "wrap all primitives and
/// strings").
/// Implementa <see cref="IComparable{T}"/> porque a ordem de <c>ParticipanteId</c> é usada para
/// desempate determinístico em mais de um ponto do motor de simplificação — ver
/// "algorithm-spec.md".
/// </summary>
public readonly record struct ParticipanteId(Guid Valor) : IComparable<ParticipanteId>
{
    public static ParticipanteId NovoId() => new(Guid.NewGuid());

    public int CompareTo(ParticipanteId outro) => Valor.CompareTo(outro.Valor);

    public override string ToString() => Valor.ToString();
}
