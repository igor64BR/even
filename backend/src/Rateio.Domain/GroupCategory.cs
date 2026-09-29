namespace Rateio.Domain;

/// <summary>
/// Category of a <see cref="Group"/> (e.g. trip, shared household, couple). There's no RF that
/// enumerates a closed set of values for this yet — modeled as an enum, not a wrapped
/// <see cref="string"/>, because T15 needs a value to populate <c>Group.Category</c> and a
/// closed set is richer than free text; adjusting/expanding the values is a UX decision for
/// another task, not this domain model's.
/// </summary>
public enum GroupCategory
{
    Trip,
    Household,
    Couple,
    Event,
    Other,
}
