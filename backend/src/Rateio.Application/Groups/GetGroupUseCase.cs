using Rateio.Application.Expenses;

namespace Rateio.Application.Groups;

/// <summary>
/// T28.3: <c>GET /groups/{id}</c> — the full group (name, category, participants, expenses),
/// closing the gap reported by T22: joining via a link (<c>POST /groups/join/{code}</c>, T21) only
/// returns <c>{groupId}</c>, with no way for the app to fetch the rest of the group's data
/// afterward. Same access pattern as <see cref="Simplification.GetDebtSimplificationUseCase"/>
/// (T32) and <see cref="Notifications.GetGroupEventsUseCase"/> (T39.1): validates RNF07 via
/// <see cref="GroupAccessVerification"/> before any read —
/// <see cref="GroupNotFoundException"/> (404) / <see cref="AccessDeniedException"/> (403).
///
/// Reuses <see cref="IGroupRepository.GetForEntryAsync"/> (T21.2) for name/category/participants
/// and <see cref="IExpenseRepository.GetDetailedByGroupAsync"/> (T28.3) for expenses, instead of
/// introducing a third group read with its own shape — see <see cref="FullGroup"/> for why
/// composing the two is already enough.
/// </summary>
public sealed class GetGroupUseCase(
    IGroupRepository groupRepository,
    IExpenseRepository expenseRepository)
{
    public async Task<FullGroup> ExecuteAsync(
        Guid authenticatedUserId,
        Guid groupId,
        CancellationToken cancellationToken = default)
    {
        await GroupAccessVerification.RequireAsync(groupRepository, groupId, authenticatedUserId, cancellationToken);

        var groupEntryInfo = await groupRepository.GetForEntryAsync(groupId, cancellationToken)
            ?? throw new GroupNotFoundException(groupId);
        var expenses = await expenseRepository.GetDetailedByGroupAsync(groupId, cancellationToken);

        return new FullGroup(groupEntryInfo, expenses);
    }
}
