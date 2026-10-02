namespace Even.Domain;

/// <summary>
/// Record that <see cref="PayerId"/> paid <see cref="Amount"/> to <see cref="PayeeId"/>
/// to settle (part of) an existing debt.
/// </summary>
public sealed record Settlement(
    Guid Id,
    ParticipantId PayerId,
    ParticipantId PayeeId,
    Money Amount);
