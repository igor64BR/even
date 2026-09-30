using Microsoft.EntityFrameworkCore;
using Tally.Application.Settlements;
using Tally.Domain;
using Tally.Infrastructure.Persistence;

namespace Tally.Infrastructure.Settlements;

/// <summary>
/// Implementation of <see cref="ISettlementRepository"/> via EF Core / <see cref="AppDbContext"/> —
/// same pattern as <c>Tally.Infrastructure.Expenses.ExpenseRepository</c>:
/// <c>AsNoTracking</c> (read-only) read, filtered by <c>GroupId</c>, whose access was already
/// validated by the caller (<c>GetDebtSimplificationUseCase</c>/<c>RegisterSettlementUseCase</c>)
/// before any of these methods is invoked.
/// </summary>
public sealed class SettlementRepository(AppDbContext dbContext) : ISettlementRepository
{
    public async Task<IReadOnlyList<Settlement>> GetByGroupAsync(
        Guid groupId, CancellationToken cancellationToken = default)
    {
        var entities = await dbContext.Settlements
            .AsNoTracking()
            .Where(settlement => settlement.GroupId == groupId)
            .ToListAsync(cancellationToken);

        return entities.Select(SettlementEntityToDomainMapper.Build).ToList();
    }

    /// <summary>
    /// Inserts the settlement directly via the FK (<c>SettlementEntity.GroupId</c>), without
    /// loading the entire <c>GroupEntity</c> just to attach one more row — same decision as
    /// <c>ExpenseRepository.AddAsync</c>, for the same reason: the group has already been
    /// confirmed to exist by <c>IGroupRepository.GetAccessAsync</c> before this method is called.
    /// </summary>
    public async Task AddAsync(Guid groupId, Settlement settlement, CancellationToken cancellationToken = default)
    {
        var entity = SettlementEntityMapper.Build(settlement);
        entity.GroupId = groupId;

        dbContext.Settlements.Add(entity);
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    /// <summary>
    /// Same pattern as <c>ExpenseRepository.GetOccurredSinceAsync</c> — <c>AsNoTracking</c>
    /// read projected directly in the query, filtered by group and by <c>CreatedAt</c> after
    /// <paramref name="since"/>. Access to the group was already validated by the caller
    /// (<c>Notifications.GetGroupEventsUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<SettlementOccurred>> GetOccurredSinceAsync(
        Guid groupId, DateTimeOffset since, CancellationToken cancellationToken = default)
    {
        return await dbContext.Settlements
            .AsNoTracking()
            .Where(settlement => settlement.GroupId == groupId && settlement.CreatedAt > since)
            .Select(settlement => new SettlementOccurred(
                settlement.Id,
                settlement.PayerId,
                settlement.PayeeId,
                settlement.AmountCents,
                settlement.CreatedAt))
            .ToListAsync(cancellationToken);
    }
}
