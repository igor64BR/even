namespace Rateio.Domain;

/// <summary>
/// A transaction suggested by the simplification engine: "<see cref="From"/> should pay
/// <see cref="Amount"/> to <see cref="To"/>".
/// </summary>
public sealed record Transaction(ParticipantId From, ParticipantId To, Money Amount);
