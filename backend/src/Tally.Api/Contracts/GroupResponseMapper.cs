using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Domain;

namespace Tally.Api.Contracts;

/// <summary>
/// Domain/Application -> Response mapping of <see cref="FullGroup"/> to
/// <see cref="GetGroupResponse"/>. Mirrors, in the opposite direction, the same per-subtype
/// translation of <see cref="ExpenseSplit"/> that <c>Tally.Infrastructure.Expenses.ExpenseEntityMapper</c>
/// already does Domain->EF — one named method per step, none doing the whole translation at once
/// (same Object Calisthenics style the project's other mappers follow).
/// </summary>
internal static class GroupResponseMapper
{
    public static GetGroupResponse Build(FullGroup fullGroup) => new(
        fullGroup.Group.Name.Value,
        fullGroup.Group.Category,
        BuildParticipants(fullGroup.Group.Participants),
        BuildExpenses(fullGroup.Expenses));

    private static List<ParticipantResponse> BuildParticipants(IReadOnlyList<Participant> participants) =>
        participants
            .Select(participant => new ParticipantResponse(
                participant.Id.Value, participant.Name.Value, participant.IsGuest))
            .ToList();

    private static List<DetailedExpenseResponse> BuildExpenses(IReadOnlyList<ExpenseToPersist> expenses) =>
        expenses.Select(BuildExpense).ToList();

    private static DetailedExpenseResponse BuildExpense(ExpenseToPersist expenseToPersist)
    {
        var expense = expenseToPersist.Expense;

        return new DetailedExpenseResponse(
            expense.Id,
            expenseToPersist.Description,
            expense.TotalAmount.Cents,
            expense.PayerId.Value,
            expenseToPersist.Date,
            GetSplitType(expense.Splits),
            BuildSplits(expense.Splits));
    }

    /// <summary>
    /// All splits of a given expense are of the same concrete <see cref="ExpenseSplit"/> subtype
    /// (invariant guaranteed at the entry edge — see that type's XML comment), so the first one's
    /// type already identifies the whole expense's.
    /// </summary>
    private static SplitTypeRequest GetSplitType(IReadOnlyList<ExpenseSplit> splits) =>
        splits[0] switch
        {
            ExpenseSplit.Equal => SplitTypeRequest.Equal,
            ExpenseSplit.Weighted => SplitTypeRequest.Weighted,
            ExpenseSplit.FixedAmount => SplitTypeRequest.FixedAmount,
            var unsupported => throw new NotSupportedException(
                $"Unsupported split type: {unsupported.GetType().Name}"),
        };

    private static List<ExpenseSplitResponse> BuildSplits(IReadOnlyList<ExpenseSplit> splits) =>
        splits.Select(BuildSplit).ToList();

    private static ExpenseSplitResponse BuildSplit(ExpenseSplit split) =>
        split switch
        {
            ExpenseSplit.Equal equal =>
                new ExpenseSplitResponse(equal.ParticipantId.Value, Weight: null, AmountCents: null),
            ExpenseSplit.Weighted weighted =>
                new ExpenseSplitResponse(weighted.ParticipantId.Value, weighted.Weight, AmountCents: null),
            ExpenseSplit.FixedAmount fixedAmount =>
                new ExpenseSplitResponse(fixedAmount.ParticipantId.Value, Weight: null, fixedAmount.Amount.Cents),
            var unsupported => throw new NotSupportedException(
                $"Unsupported split type: {unsupported.GetType().Name}"),
        };
}
