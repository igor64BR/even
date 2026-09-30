using Tally.Domain;

namespace Tally.Application.Settlements;

/// <summary>
/// Persistence of settlements — same pattern as
/// <c>Tally.Application.Expenses.IExpenseRepository</c>: its own abstraction (Interface
/// Segregation) instead of hanging settlement read/write off an interface designed for another
/// entity. The read method is what
/// <c>GetDebtSimplificationUseCase</c> needs to feed the simplification engine with a
/// group's current settlement history. The write method records that a suggested
/// transaction was paid, without breaking whoever already depends on the read side. Concrete
/// implementation (EF Core) in Tally.Infrastructure.
/// </summary>
public interface ISettlementRepository
{
    /// <summary>
    /// All current settlements of a group, already as the domain type. <paramref name="groupId"/>
    /// was already validated by the caller — this method does not revalidate the group's
    /// existence or access.
    /// </summary>
    Task<IReadOnlyList<Settlement>> GetByGroupAsync(Guid groupId, CancellationToken cancellationToken = default);

    /// <summary>
    /// Persists <paramref name="settlement"/> as belonging to group <paramref name="groupId"/>
    /// (already validated by the caller — this method does not revalidate the group's existence or
    /// access). Same contract as <c>IExpenseRepository.AddAsync</c>: does not recompute or store a
    /// balance, that's on demand via the next <c>GetByGroupAsync</c> read.
    /// </summary>
    Task AddAsync(Guid groupId, Settlement settlement, CancellationToken cancellationToken = default);

    /// <summary>
    /// Settlements of the group persisted on the server after <paramref name="since"/>
    /// (compared against <c>CreatedAt</c>). Same role as
    /// <c>Expenses.IExpenseRepository.GetOccurredSinceAsync</c>: its own projection for the pull
    /// fallback (<see cref="SettlementOccurred"/>), used only by
    /// <c>Notifications.GetGroupEventsUseCase</c> — not the domain <see cref="Settlement"/> that
    /// <see cref="GetByGroupAsync"/> returns for the simplification engine.
    /// <paramref name="groupId"/> was already validated by the caller.
    /// </summary>
    Task<IReadOnlyList<SettlementOccurred>> GetOccurredSinceAsync(
        Guid groupId, DateTimeOffset since, CancellationToken cancellationToken = default);
}
