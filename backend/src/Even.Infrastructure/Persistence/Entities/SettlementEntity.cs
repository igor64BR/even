namespace Even.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a settlement. <c>GET /groups/{id}/settlement</c> adds current settlements
/// into the group's balance, the same rule the simplification engine already applies in
/// <c>ComputeBalances</c>.
///
/// <see cref="AmountCents"/> stores the same raw value that <see cref="Even.Domain.Money.Cents"/>
/// exposes, the same decision as <see cref="ExpenseEntity.TotalAmountCents"/> — it never becomes a
/// raw <c>decimal</c> in the column.
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
