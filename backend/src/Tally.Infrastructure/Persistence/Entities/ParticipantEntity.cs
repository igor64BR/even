namespace Tally.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a group participant. Minimal skeleton (T2.2) — not to be confused with
/// <see cref="Tally.Domain.ParticipantId"/>, which is just the wrapped identifier the
/// simplification engine uses; this entity stores the full record (name, link to a group).
/// <see cref="IsGuest"/> was added in T18: it mirrors
/// <see cref="Tally.Domain.Participant.IsGuest"/> so the guest/authenticated distinction (RF06/RF07)
/// survives the domain-to-EF-to-domain round trip.
/// </summary>
public class ParticipantEntity
{
    public Guid Id { get; set; }

    public Guid GroupId { get; set; }

    public string Name { get; set; } = string.Empty;

    public bool IsGuest { get; set; }

    public GroupEntity? Group { get; set; }
}
