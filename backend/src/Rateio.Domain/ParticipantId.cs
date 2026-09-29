namespace Rateio.Domain;

/// <summary>
/// Stable identifier of a participant. Wraps the raw value (<see cref="Guid"/>) instead of
/// letting it circulate raw through the domain (Object Calisthenics: "wrap all primitives and
/// strings").
/// Implements <see cref="IComparable{T}"/> because <c>ParticipantId</c> ordering is used for
/// deterministic tie-breaking in more than one place in the simplification engine — see
/// "algorithm-spec.md".
/// </summary>
public readonly record struct ParticipantId(Guid Value) : IComparable<ParticipantId>
{
    public static ParticipantId New() => new(Guid.NewGuid());

    public int CompareTo(ParticipantId other) => Value.CompareTo(other.Value);

    public override string ToString() => Value.ToString();
}
