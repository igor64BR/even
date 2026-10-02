using Even.Domain;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Expenses;

/// <summary>
/// EF-to-domain mapping of an expense: the opposite direction of
/// <see cref="ExpenseEntityMapper"/> (domain-to-EF). Before the settlement endpoint existed, no use
/// case needed to reconstruct the domain <see cref="Expense"/> from what's persisted — only persist
/// it; the settlement endpoint (<c>GET /groups/{id}/settlement</c>) is the first consumer, because
/// the simplification engine only understands the domain type, never <see cref="ExpenseEntity"/>.
///
/// Reconstructs the concrete <see cref="ExpenseSplit"/> subtype from the <see cref="SplitTypeEntity"/>
/// discriminator — the exact inverse of <c>ExpenseEntityMapper.BuildSplit</c>. One named method per
/// step, with no single method doing the whole translation at once (same Object Calisthenics
/// already followed on the opposite side).
/// </summary>
internal static class ExpenseEntityToDomainMapper
{
    public static Expense Build(ExpenseEntity entity) => new(
        entity.Id,
        Money.FromCents(entity.TotalAmountCents),
        new ParticipantId(entity.PayerId),
        BuildSplits(entity.Splits));

    private static List<ExpenseSplit> BuildSplits(
        IReadOnlyList<ExpenseSplitEntity> splits) =>
        splits.Select(BuildSplit).ToList();

    private static ExpenseSplit BuildSplit(ExpenseSplitEntity entity)
    {
        var participantId = new ParticipantId(entity.ParticipantId);

        return entity.Type switch
        {
            SplitTypeEntity.Equal => new ExpenseSplit.Equal(participantId),
            SplitTypeEntity.Weighted => new ExpenseSplit.Weighted(participantId, RequireWeight(entity)),
            SplitTypeEntity.FixedAmount => new ExpenseSplit.FixedAmount(participantId, RequireAmount(entity)),
            var unsupported => throw new NotSupportedException(
                $"Unsupported split type: {unsupported}"),
        };
    }

    private static long RequireWeight(ExpenseSplitEntity entity) =>
        entity.Weight ?? throw new InvalidOperationException(
            $"Split {entity.Id} persisted as Weighted without a weight — inconsistent data in the database.");

    private static Money RequireAmount(ExpenseSplitEntity entity) =>
        entity.AmountCents is { } amountCents
            ? Money.FromCents(amountCents)
            : throw new InvalidOperationException(
                $"Split {entity.Id} persisted as FixedAmount without an amount — inconsistent data in the database.");
}
