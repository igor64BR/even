using Even.Application.Groups;
using Even.Domain;

namespace Even.Application.Expenses;

/// <summary>
/// DTO-to-domain mapping of an expense, extracted from
/// <c>Even.Application.Groups.SyncedGroupBuilder</c> to be reused by
/// <c>POST /groups/{id}/expenses</c> (a single new expense) without duplicating logic that
/// already existed for the bulk sync case. Still reuses the expense DTOs from
/// <see cref="Even.Application.Groups"/> (<see cref="SyncedExpenseRequest"/> and friends) instead
/// of creating a second set of types with the same shape — the payload of "one new expense" is
/// structurally identical to "one expense inside the sync bulk".
///
/// One named method per step, with no single method doing the whole translation at once (Object
/// Calisthenics) — and without bypassing domain validation: whoever validates the
/// <see cref="ExpenseSplit"/> subtypes is still the domain constructors.
/// </summary>
internal static class ExpenseMapper
{
    public static Expense Build(SyncedExpenseRequest request) =>
        new(
            request.Id,
            Money.FromCents(request.TotalAmountCents),
            new ParticipantId(request.PayerId),
            BuildSplits(request.SplitType, request.Splits));

    private static List<ExpenseSplit> BuildSplits(
        SplitTypeRequest type, IReadOnlyList<SyncedExpenseSplitRequest> splits) =>
        splits.Select(split => BuildSplit(type, split)).ToList();

    private static ExpenseSplit BuildSplit(
        SplitTypeRequest type, SyncedExpenseSplitRequest request)
    {
        var participantId = new ParticipantId(request.ParticipantId);

        return type switch
        {
            SplitTypeRequest.Equal => new ExpenseSplit.Equal(participantId),
            SplitTypeRequest.Weighted => new ExpenseSplit.Weighted(participantId, RequireWeight(request)),
            SplitTypeRequest.FixedAmount => new ExpenseSplit.FixedAmount(participantId, RequireAmount(request)),
            _ => throw new ArgumentOutOfRangeException(nameof(type), type, "Unsupported split type."),
        };
    }

    private static long RequireWeight(SyncedExpenseSplitRequest request) =>
        request.Weight ?? throw new ArgumentException(
            "Weight is required for an expense split divided by weight.", nameof(request));

    private static Money RequireAmount(SyncedExpenseSplitRequest request) =>
        request.AmountCents is { } amountCents
            ? Money.FromCents(amountCents)
            : throw new ArgumentException(
                "Amount is required for an expense split divided by a fixed amount.", nameof(request));
}
