namespace Even.Domain;

/// <summary>
/// Participant of a <see cref="Group"/>. The distinction between a guest (name only, no
/// account) and an authenticated one (linked to a Google account) is central to the product,
/// so it's expressed as two named
/// factory methods instead of a constructor taking a loose <see cref="bool"/> (Object
/// Calisthenics: a boolean parameter at the call site doesn't say what's being created;
/// <c>Participant.Guest(...)</c> does).
/// </summary>
public sealed class Participant : IEquatable<Participant>
{
    public ParticipantId Id { get; }

    public ParticipantName Name { get; }

    /// <summary>
    /// <c>true</c> for a participant added by name only, with no account or app of their
    /// own; <c>false</c> for a participant linked to a Google account.
    /// </summary>
    public bool IsGuest { get; }

    private Participant(ParticipantId id, ParticipantName name, bool isGuest)
    {
        Id = id;
        Name = name;
        IsGuest = isGuest;
    }

    public static Participant Guest(ParticipantId id, ParticipantName name) =>
        new(id, name, isGuest: true);

    public static Participant Authenticated(ParticipantId id, ParticipantName name) =>
        new(id, name, isGuest: false);

    public bool Equals(Participant? other)
    {
        if (other is null)
        {
            return false;
        }

        return Id.Equals(other.Id);
    }

    public override bool Equals(object? obj) => Equals(obj as Participant);

    public override int GetHashCode() => Id.GetHashCode();
}
