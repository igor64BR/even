using Tally.Domain;
using Tally.Infrastructure.Persistence.Entities;

namespace Tally.Infrastructure.Settlements;

/// <summary>
/// Domain-to-EF mapping of a settlement, mirroring
/// <c>Tally.Infrastructure.Expenses.ExpenseEntityMapper</c> on the expense side and
/// <see cref="SettlementEntityToDomainMapper"/> in the opposite direction of this same
/// entity. Doesn't set <see cref="SettlementEntity.GroupId"/> — the caller
/// (<see cref="SettlementRepository"/>) assigns the FK directly, same pattern as the single-expense
/// insert.
/// </summary>
internal static class SettlementEntityMapper
{
    public static SettlementEntity Build(Settlement settlement) => new()
    {
        Id = settlement.Id,
        PayerId = settlement.PayerId.Value,
        PayeeId = settlement.PayeeId.Value,
        AmountCents = settlement.Amount.Cents,
        CreatedAt = DateTimeOffset.UtcNow,
    };
}
