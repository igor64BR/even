using Tally.Application.Groups;

namespace Tally.Application.Expenses;

/// <summary>
/// T28.1: <c>PUT /groups/{id}/expenses/{expenseId}</c> — edits an already-persisted expense
/// (RF20). Same access pattern as <see cref="CreateExpenseUseCase"/> (T23): (1) RNF07 via
/// <see cref="GroupAccessVerification"/> — <see cref="GroupNotFoundException"/> if the group
/// doesn't exist (404), <see cref="AccessDeniedException"/> if it exists but the authenticated
/// user doesn't have access to it (403); only then (2) the payload becomes a full domain
/// <see cref="Domain.Expense"/> via <see cref="ExpenseMapper"/> — the same invariant validation
/// <see cref="CreateExpenseUseCase"/> applies, never a partial loose-field update — and (3) it's
/// persisted via <see cref="IExpenseRepository.UpdateAsync"/>, which returns <c>false</c> (without
/// throwing) when the expense doesn't exist in that group — becomes <see cref="ExpenseNotFoundException"/>
/// (404) here.
///
/// <paramref name="expenseId"/> always comes from the route, never from the request body — the
/// same structural care as RNF07 (never trust identity coming from the payload): the <c>Id</c> of
/// <paramref name="request"/> is replaced by the one from the route before any mapping, so there's
/// no way for the request body to edit an expense other than the one the URL points to.
///
/// Does not recompute or store a balance (same reasoning as T35.2): the next call to
/// <c>GET /groups/{id}/settlement</c> already recomputes on demand from the current history.
/// </summary>
public sealed class EditExpenseUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository)
{
    public async Task ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        Guid expenseId,
        SyncedExpenseRequest request,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var requestWithRouteId = request with { Id = expenseId };
        var expense = ExpenseMapper.Build(requestWithRouteId);
        var expenseToPersist = new ExpenseToPersist(
            expense, requestWithRouteId.Description, requestWithRouteId.Date);

        var updated = await expenseRepository.UpdateAsync(groupId, expenseToPersist, cancellationToken);
        if (!updated)
        {
            throw new ExpenseNotFoundException(groupId, expenseId);
        }
    }
}
