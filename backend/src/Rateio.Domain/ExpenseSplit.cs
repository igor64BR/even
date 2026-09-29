namespace Rateio.Domain;

/// <summary>
/// A participant's share of an <see cref="Expense"/>. The concrete type already carries the
/// applicable split rule (equal, weighted, or fixed amount) — there's no separate
/// <c>SplitType</c> enum holding that information again, because that would leave two places
/// that could drift apart (the enum saying one thing, the items in
/// <see cref="Expense.Splits"/> being of another type). All splits of a given expense must be
/// of the same concrete subtype; validating that consistency is the entry edge's
/// (DTO/form) responsibility, not this type's or the engine's.
/// </summary>
public abstract record ExpenseSplit(ParticipantId ParticipantId)
{
    /// <summary>Equal split among all participants (RF17).</summary>
    public sealed record Equal(ParticipantId ParticipantId) : ExpenseSplit(ParticipantId);

    /// <summary>Split proportional to a weight/percentage per participant (RF18).</summary>
    public sealed record Weighted(ParticipantId ParticipantId, long Weight) : ExpenseSplit(ParticipantId);

    /// <summary>Split by a fixed amount defined per participant (RF19).</summary>
    public sealed record FixedAmount(ParticipantId ParticipantId, Money Amount) : ExpenseSplit(ParticipantId);
}
