using Rateio.Application.Expenses;
using Rateio.Domain;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Expenses;

/// <summary>
/// Domain-to-EF mapping of an expense, extracted from
/// <c>Rateio.Infrastructure.Groups.GroupRepository</c> (T18.1) to be reused by T23
/// (<see cref="ExpenseRepository"/>), which persists a single new expense in a group that already
/// exists instead of an entire group graph. Mirrors
/// <c>Rateio.Application.Expenses.ExpenseMapper</c> on the opposite side of the translation (Object
/// Calisthenics: small, named methods, one per step).
///
/// Doesn't set <see cref="ExpenseEntity.GroupId"/> — the returned entity isn't associated with a
/// group yet; the caller decides how (fixup via navigation collection in the sync bulk load, a
/// direct FK on <see cref="ExpenseRepository"/>'s single-expense insert).
/// </summary>
internal static class ExpenseEntityMapper
{
    public static ExpenseEntity Build(ExpenseToPersist expenseToPersist) => new()
    {
        Id = expenseToPersist.Expense.Id,
        PayerId = expenseToPersist.Expense.PayerId.Value,
        TotalAmountCents = expenseToPersist.Expense.TotalAmount.Cents,
        Description = expenseToPersist.Description,
        Date = expenseToPersist.Date,
        CreatedAt = DateTimeOffset.UtcNow,
        Splits = BuildSplits(expenseToPersist.Expense.Splits),
    };

    private static List<ExpenseSplitEntity> BuildSplits(
        IReadOnlyList<ExpenseSplit> splits) =>
        splits.Select(BuildSplit).ToList();

    private static ExpenseSplitEntity BuildSplit(ExpenseSplit split) =>
        split switch
        {
            ExpenseSplit.Equal equal => new ExpenseSplitEntity
            {
                Id = Guid.NewGuid(),
                ParticipantId = equal.ParticipantId.Value,
                Type = SplitTypeEntity.Equal,
            },
            ExpenseSplit.Weighted weighted => new ExpenseSplitEntity
            {
                Id = Guid.NewGuid(),
                ParticipantId = weighted.ParticipantId.Value,
                Type = SplitTypeEntity.Weighted,
                Weight = weighted.Weight,
            },
            ExpenseSplit.FixedAmount fixedAmount => new ExpenseSplitEntity
            {
                Id = Guid.NewGuid(),
                ParticipantId = fixedAmount.ParticipantId.Value,
                Type = SplitTypeEntity.FixedAmount,
                AmountCents = fixedAmount.Amount.Cents,
            },
            var unsupported => throw new NotSupportedException(
                $"Unsupported split type: {unsupported.GetType().Name}"),
        };
}
