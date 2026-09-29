namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of an expense. Minimal skeleton at first (T2.2); T18 added
/// <see cref="Date"/> (the entry date reported by the app — distinct from <see cref="CreatedAt"/>,
/// which is the timestamp of when the row reached the server) and <see cref="Splits"/> (the
/// per-participant split, RF17/18/19), because T18 is the first time the project persists a full
/// <see cref="Rateio.Domain.Expense"/>.
///
/// <see cref="TotalAmountCents"/> stores the same raw value that
/// <see cref="Rateio.Domain.Money.Cents"/> exposes — the column never becomes a raw
/// <c>decimal</c>; this preserves exact reconstruction of <see cref="Rateio.Domain.Money"/> from
/// the saved data (see T2's guidance and algorithm-spec.md). <see cref="PayerId"/> corresponds to
/// the payer's <see cref="Rateio.Domain.ParticipantId.Value"/>.
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
