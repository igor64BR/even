namespace Tally.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of an expense. <see cref="Date"/> is the entry date reported by the app —
/// distinct from <see cref="CreatedAt"/>, which is the timestamp of when the row reached the
/// server. <see cref="Splits"/> holds the per-participant split.
///
/// <see cref="TotalAmountCents"/> stores the same raw value that
/// <see cref="Tally.Domain.Money.Cents"/> exposes — the column never becomes a raw
/// <c>decimal</c>; this preserves exact reconstruction of <see cref="Tally.Domain.Money"/> from
/// the saved data. <see cref="PayerId"/> corresponds to
/// the payer's <see cref="Tally.Domain.ParticipantId.Value"/>.
/// </summary>
public class ExpenseEntity
{
    public Guid Id { get; set; }

    public Guid GroupId { get; set; }

    public Guid PayerId { get; set; }

    public long TotalAmountCents { get; set; }

    public string Description { get; set; } = string.Empty;

    public DateOnly Date { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public GroupEntity? Group { get; set; }

    public List<ExpenseSplitEntity> Splits { get; set; } = [];
}
