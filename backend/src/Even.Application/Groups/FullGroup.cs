using Even.Application.Expenses;

namespace Even.Application.Groups;

/// <summary>
/// Output of <see cref="GetGroupUseCase"/> — everything <c>GET /groups/{id}</c> needs to
/// populate the app's screen after joining via a link (<c>POST /groups/join/{code}</c> only
/// returns <c>{groupId}</c>, with no way for the app to fetch the rest of the group's data
/// afterward). Composition of two already-established types instead
/// of a parallel shape: <see cref="Group"/> reuses the same minimal name/category/participants read
/// that <see cref="JoinGroupViaInviteUseCase"/> already uses (<see cref="GroupEntryInfo"/>),
/// and <see cref="Expenses"/> reuses <see cref="ExpenseToPersist"/> — the same type used to
/// persist an expense, returned here through the read path
/// <see cref="IExpenseRepository.GetDetailedByGroupAsync"/>.
/// </summary>
public sealed record FullGroup(GroupEntryInfo Group, IReadOnlyList<ExpenseToPersist> Expenses);
