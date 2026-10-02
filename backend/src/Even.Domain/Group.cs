namespace Even.Domain;

/// <summary>
/// Aggregate root of an expense-splitting group. Protected invariant: a group needs at
/// least 1 participant to exist — the app allows creating one with just "You", but never with
/// zero. The invariant is enforced on creation
/// (<see cref="Create"/>) and re-validated on every mutation that shrinks the participant list
/// (<see cref="RemoveParticipant"/>); there are no loose public setters, so there's no path to
/// violate this after creation.
/// </summary>
public sealed class Group
{
    private readonly List<Participant> _participants;

    public Guid Id { get; }

    public GroupName Name { get; }

    public GroupCategory Category { get; }

    /// <summary>
    /// <c>true</c> when the group has been synced with the backend; local groups are
    /// born <c>false</c> and only change state via <see cref="MarkAsSynced"/>.
    /// </summary>
    public bool Synced { get; private set; }

    public IReadOnlyList<Participant> Participants => _participants;

    private Group(
        Guid id,
        GroupName name,
        GroupCategory category,
        List<Participant> participants,
        bool synced)
    {
        Id = id;
        Name = name;
        Category = category;
        _participants = participants;
        Synced = synced;
    }

    /// <summary>
    /// The only way to create a <see cref="Group"/> — guarantees the at-least-1-participant
    /// invariant instead of leaving it up to whoever calls a public constructor.
    /// </summary>
    public static Group Create(
        GroupName name,
        GroupCategory category,
        IReadOnlyCollection<Participant> participants)
    {
        ArgumentNullException.ThrowIfNull(participants);
        RequireAtLeastOneParticipant(participants.Count);

        return new Group(Guid.NewGuid(), name, category, new List<Participant>(participants), synced: false);
    }

    public void AddParticipant(Participant participant)
    {
        ArgumentNullException.ThrowIfNull(participant);

        if (_participants.Any(existing => existing.Id.Equals(participant.Id)))
        {
            throw new InvalidOperationException($"Participant {participant.Id} is already in the group.");
        }

        _participants.Add(participant);
    }

    /// <summary>
    /// Removes a participant by id. A zero-balance check before removal depends on
    /// expenses/balances, which this aggregate doesn't know about (that's the responsibility of
    /// an application layer that also sees <see cref="Expense"/>); this only
    /// protects <see cref="Group"/>'s own structural invariant: never end up with zero
    /// participants.
    /// </summary>
    public void RemoveParticipant(ParticipantId participantId)
    {
        RequireAtLeastOneParticipant(_participants.Count - 1);

        var remainingParticipants = _participants.Where(p => !p.Id.Equals(participantId)).ToList();
        if (remainingParticipants.Count == _participants.Count)
        {
            throw new InvalidOperationException($"Participant {participantId} is not in the group.");
        }

        _participants.Clear();
        _participants.AddRange(remainingParticipants);
    }

    public void MarkAsSynced() => Synced = true;

    private static void RequireAtLeastOneParticipant(int count)
    {
        if (count < 1)
        {
            throw new InvalidOperationException("A group needs at least 1 participant.");
        }
    }
}
