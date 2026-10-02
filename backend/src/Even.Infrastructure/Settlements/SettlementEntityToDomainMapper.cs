using Even.Domain;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Settlements;

/// <summary>
/// EF-to-domain mapping of a settlement — mirrors
/// <see cref="Expenses.ExpenseEntityToDomainMapper"/> on the settlement side. With no
/// subtypes/discriminator to reconstruct (unlike <see cref="Domain.ExpenseSplit"/>), the
/// translation is direct enough to not need private per-step methods.
/// </summary>
internal static class SettlementEntityToDomainMapper
{
    public static Settlement Build(SettlementEntity entity) => new(
        entity.Id,
        new ParticipantId(entity.PayerId),
        new ParticipantId(entity.PayeeId),
        Money.FromCents(entity.AmountCents));
}
