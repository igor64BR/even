namespace Tally.Domain;

/// <summary>
/// Category of a <see cref="Group"/> (e.g. trip, shared household, couple). Modeled as an enum,
/// not a wrapped <see cref="string"/>, because a closed set is richer than free text;
/// adjusting/expanding the values is a UX decision, not this domain model's.
/// </summary>
public enum GroupCategory
{
    Trip,
    Household,
    Couple,
    Event,
    Other,
}
