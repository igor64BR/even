namespace Tally.Domain;

/// <summary>
/// Name of a <see cref="Participant"/>, wrapped instead of a raw <see cref="string"/> (Object
/// Calisthenics: "wrap all primitives and strings"). For a guest participant (RF06 — spec.md)
/// the name is the only data that exists, so validating that it's never empty here protects the
/// only information that identifies that person within the group.
/// </summary>
public readonly record struct ParticipantName
{
    public string Value { get; }

    private ParticipantName(string value)
    {
        Value = value;
    }

    public static ParticipantName Create(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException("Participant name cannot be empty.", nameof(value));
        }

        return new ParticipantName(value.Trim());
    }

    public override string ToString() => Value;
}
