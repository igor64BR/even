using Rateio.Application.Groups;

namespace Rateio.Application.Expenses;

/// <summary>
/// T28.2: <c>DELETE /groups/{id}/expenses/{expenseId}</c> — deletes an already-persisted expense
/// (RF21). Same access pattern as <see cref="CreateExpenseUseCase"/>/<see cref="EditExpenseUseCase"/>:
/// RNF07 via <see cref="GroupAccessVerification"/> before anything else —
/// <see cref="GroupNotFoundException"/> (404) / <see cref="AccessDeniedException"/> (403) — only
/// then it removes via <see cref="IExpenseRepository.RemoveAsync"/>, which returns <c>false</c>
/// (without throwing) when the expense doesn't exist in that group, becoming
/// <see cref="ExpenseNotFoundException"/> (404) here.
///
/// Does not recompute or store a balance — same reasoning as <see cref="EditExpenseUseCase"/>: the
/// next call to <c>GET /groups/{id}/settlement</c> already recomputes on demand.
/// </summary>
public sealed class DeleteExpenseUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository)
{
    public async Task ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        Guid expenseId,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var removed = await expenseRepository.RemoveAsync(groupId, expenseId, cancellationToken);
        if (!removed)
        {
            throw new ExpenseNotFoundException(groupId, expenseId);
        }
    }
}
