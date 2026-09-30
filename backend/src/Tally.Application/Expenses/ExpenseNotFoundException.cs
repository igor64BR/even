namespace Tally.Application.Expenses;

/// <summary>
/// The referenced expense (<c>PUT</c>/<c>DELETE /groups/{id}/expenses/{expenseId}</c>)
/// does not exist in that group — the group may exist (access already validated by
/// <see cref="Groups.GroupAccessVerification"/> before this check), just not the expense. Never
/// becomes a 500 — the controller maps this to 404, same pattern as
/// <see cref="Groups.GroupNotFoundException"/>.
/// </summary>
public sealed class ExpenseNotFoundException(Guid groupId, Guid expenseId)
    : Exception($"Expense {expenseId} not found in group {groupId}.")
{
    public Guid GroupId { get; } = groupId;

    public Guid ExpenseId { get; } = expenseId;
}
