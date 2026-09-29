using Rateio.Domain;

namespace Rateio.Application.Expenses;

/// <summary>
/// Persistence of a single expense within a group that already exists (T23) — distinct from
/// <c>Rateio.Application.Groups.IGroupRepository</c>, which persists a group's entire graph on the
/// first sync (T18). Separating the two interfaces keeps this "day-to-day" use case from depending
/// on an abstraction designed for the bulk scenario (Interface Segregation). Concrete
/// implementation (EF Core) in Rateio.Infrastructure.
/// </summary>
public interface IExpenseRepository
{
    /// <summary>
    /// Adds <paramref name="expense"/> to a group that already exists on the server
    /// (<paramref name="groupId"/> was already validated by the caller — this method does not
    /// revalidate the group's existence or RNF07). Does not recompute or store a balance: that's
    /// on demand, via T32.
    /// </summary>
    Task AddAsync(Guid groupId, ExpenseToPersist expense, CancellationToken cancellationToken = default);

    /// <summary>
    /// T32: all current expenses of a group, already as the domain type (not
    /// <see cref="ExpenseToPersist"/> — whoever reads this back into the simplification engine
    /// doesn't need description/entry date, only what <c>ComputeBalances</c> consumes).
    /// <paramref name="groupId"/> was already validated by the caller (RNF07), same as
    /// <see cref="AddAsync"/>.
    /// </summary>
    Task<IReadOnlyList<Expense>> GetByGroupAsync(Guid groupId, CancellationToken cancellationToken = default);

    /// <summary>
    /// T39.1: expenses of the group persisted on the server after <paramref name="since"/>
    /// (compared against <c>CreatedAt</c> — the server timestamp, not <c>Date</c>, the entry date
    /// reported by the app, which only has day granularity and isn't usable as a sync cursor). Its
    /// own projection for the pull fallback (<see cref="ExpenseOccurred"/>), not the domain
    /// <see cref="Expense"/> that <see cref="GetByGroupAsync"/> returns for the simplification
    /// engine — this method exists only for
    /// <c>Notifications.GetGroupEventsUseCase</c> to build events, not to recompute a balance.
    /// <paramref name="groupId"/> was already validated by the caller (RNF07), same contract as
    /// <see cref="GetByGroupAsync"/>.
    /// </summary>
    Task<IReadOnlyList<ExpenseOccurred>> GetOccurredSinceAsync(
        Guid groupId, DateTimeOffset since, CancellationToken cancellationToken = default);

    /// <summary>
    /// T28.3: all current expenses of the group already including
    /// <see cref="ExpenseToPersist.Description"/>/<see cref="ExpenseToPersist.Date"/> — different
    /// from <see cref="GetByGroupAsync"/> (T32), which returns only the domain
    /// <see cref="Expense"/> because the simplification engine doesn't need description/entry
    /// date. <c>GET /groups/{id}</c> (<c>GetGroupUseCase</c>) needs both to populate the app's
    /// screen after joining via a link (gap reported by T22) — reuses <see cref="ExpenseToPersist"/>,
    /// the same type T18/T23 already use to persist, instead of a parallel read DTO.
    /// <paramref name="groupId"/> was already validated by the caller (RNF07), same contract as
    /// <see cref="GetByGroupAsync"/>.
    /// </summary>
    Task<IReadOnlyList<ExpenseToPersist>> GetDetailedByGroupAsync(
        Guid groupId, CancellationToken cancellationToken = default);

    /// <summary>
    /// T28.1: fully replaces the expense <c>expense.Expense.Id</c> within group
    /// <paramref name="groupId"/> — a complete reconstruction (never a partial loose-field
    /// update), the same invariant guarantee <see cref="AddAsync"/> already imposes via
    /// <c>ExpenseMapper</c>. Returns <c>false</c> (without throwing) when the expense doesn't
    /// exist in that group — the caller (<c>EditExpenseUseCase</c>) decides whether that becomes a
    /// 404, same pattern as <c>IGroupRepository.GetAccessAsync</c> returning <c>null</c>.
    /// </summary>
    Task<bool> UpdateAsync(Guid groupId, ExpenseToPersist expense, CancellationToken cancellationToken = default);

    /// <summary>
    /// T28.2: removes expense <paramref name="expenseId"/> from group <paramref name="groupId"/>.
    /// Returns <c>false</c> (without throwing) when it doesn't exist — same pattern as
    /// <see cref="UpdateAsync"/>.
    /// </summary>
    Task<bool> RemoveAsync(Guid groupId, Guid expenseId, CancellationToken cancellationToken = default);
}
