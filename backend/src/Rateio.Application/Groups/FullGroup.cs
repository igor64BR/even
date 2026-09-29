using Rateio.Application.Expenses;

namespace Rateio.Application.Groups;

/// <summary>
/// T28.3: output of <see cref="GetGroupUseCase"/> — everything <c>GET /groups/{id}</c> needs to
/// populate the app's screen after joining via a link (gap reported by T22:
/// <c>POST /groups/join/{code}</c> only returns <c>{groupId}</c>, with no way for the app to fetch
/// the rest of the group's data afterward). Composition of two already-established types instead
/// of a parallel shape: <see cref="Group"/> reuses the same minimal name/category/participants read
/// that <see cref="JoinGroupViaInviteUseCase"/> (T21.2) already uses (<see cref="GroupEntryInfo"/>),
/// and <see cref="Expenses"/> reuses <see cref="ExpenseToPersist"/> — the same type T18/T23 use to
/// persist an expense, returned here through the read path
/// <see cref="IExpenseRepository.GetDetailedByGroupAsync"/>.
/// </summary>
public sealed record FullGroup(GroupEntryInfo Group, IReadOnlyList<ExpenseToPersist> Expenses);
