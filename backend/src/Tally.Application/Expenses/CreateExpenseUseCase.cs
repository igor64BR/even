using Tally.Application.Groups;
using Tally.Application.Notifications;

namespace Tally.Application.Expenses;

/// <summary>
/// T23.2: adds a SINGLE new expense to a group that is already synced (RF.., a day-to-day
/// operation) — different from <see cref="SyncGroupUseCase"/> (T18), which uploads a group's
/// entire local state on the first sync.
///
/// Validation order (same as the task): (1) the group exists — otherwise <see cref="GroupNotFoundException"/>
/// (the controller maps it to 404); (2) the authenticated user has access to the group, RNF07 —
/// otherwise <see cref="AccessDeniedException"/> (403); only then (3) the payload becomes a domain
/// <see cref="Domain.Expense"/> via <see cref="ExpenseMapper"/> (the same DTO-to-domain mapping T18
/// wrote for sync, extracted to <c>Tally.Application.Expenses</c> because both use cases need it)
/// and (4) it's persisted via <see cref="IExpenseRepository"/> — without recomputing/storing a
/// balance, that's on demand via T32. (5) T38.2: once the expense is successfully persisted,
/// notify <see cref="IGroupEventNotifier"/> (RF35/RF36) — depends only on Application's
/// abstraction, never directly on SignalR (Dependency Inversion).
///
/// <paramref name="authenticatedUserId"/> is only used to check access, never written as part of
/// the expense — the same structural guarantee T18 gives for the group owner.
/// </summary>
public sealed class CreateExpenseUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository,
    IGroupEventNotifier groupEventNotifier)
{
    public async Task<Guid> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        SyncedExpenseRequest request,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var expense = ExpenseMapper.Build(request);
        var expenseToPersist = new ExpenseToPersist(expense, request.Description, request.Date);

        await expenseRepository.AddAsync(groupId, expenseToPersist, cancellationToken);

        var groupEvent = new ExpenseCreatedEvent(
            groupId,
            expense.Id,
            expenseToPersist.Description,
            expense.TotalAmount.Cents,
            expense.PayerId.Value);
        await groupEventNotifier.NotifyAsync(groupEvent, cancellationToken);

        return expense.Id;
    }
}
