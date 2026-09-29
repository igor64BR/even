namespace Tally.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a settlement (RF31/RF33). T32 creates this table only for the read
/// path — <c>GET /groups/{id}/settlement</c> adds current settlements into the group's balance, the
/// same rule the simplification engine (T31) already applies in <c>ComputeBalances</c>. Recording a
/// new settlement (the write path) is T35's scope; this entity and its configuration are already
/// ready for that, without needing a second migration for the same fields.
///
/// <see cref="AmountCents"/> stores the same raw value that <see cref="Tally.Domain.Money.Cents"/>
/// exposes, the same decision as <see cref="ExpenseEntity.TotalAmountCents"/> — it never becomes a
/// raw <c>decimal</c> in the column (algorithm-spec.md, "Money" section).
/// </summary>
public class SettlementEntity
{
    public Guid Id { get; set; }

    public Guid GroupId { get; set; }

    public Guid PayerId { get; set; }

    public Guid PayeeId { get; set; }

    public long AmountCents { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public GroupEntity? Group { get; set; }
}
