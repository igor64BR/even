using Microsoft.EntityFrameworkCore;
using Tally.Application.Expenses;
using Tally.Domain;
using Tally.Infrastructure.Persistence;
using Tally.Infrastructure.Persistence.Entities;

namespace Tally.Infrastructure.Expenses;

/// <summary>
/// Implementation of <see cref="IExpenseRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// Inserts the expense directly via the FK (<c>ExpenseEntity.GroupId</c>) instead of
/// loading the entire <c>GroupEntity</c> (existing participants/expenses) just to attach one more
/// expense — the group has already been confirmed to exist by
/// <c>IGroupRepository.GetAccessAsync</c> before this method is called
/// (<see cref="CreateExpenseUseCase"/>).
/// </summary>
public sealed class ExpenseRepository(AppDbContext dbContext) : IExpenseRepository
{
    public async Task AddAsync(
        Guid groupId, ExpenseToPersist expense, CancellationToken cancellationToken = default)
    {
        var entity = ExpenseEntityMapper.Build(expense);
        entity.GroupId = groupId;

        dbContext.Expenses.Add(entity);
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    /// <summary>
    /// <c>AsNoTracking</c> (read-only) load of all current expenses of the group, with
    /// <c>Include</c> of the splits — without them the simplification engine couldn't
    /// reconstruct each expense's split. Access to the group was already validated by the caller
    /// (<c>GetDebtSimplificationUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<Expense>> GetByGroupAsync(
        Guid groupId, CancellationToken cancellationToken = default)
    {
        var entities = await dbContext.Expenses
            .AsNoTracking()
            .Include(expense => expense.Splits)
            .Where(expense => expense.GroupId == groupId)
            .ToListAsync(cancellationToken);

        return entities.Select(ExpenseEntityToDomainMapper.Build).ToList();
    }

    /// <summary>
    /// <c>AsNoTracking</c> read projected directly in the query (no <c>Include</c> of
    /// splits — the pull fallback doesn't need the expense's split, only the summary that makes up
    /// <see cref="Notifications.ExpenseCreatedEvent"/>), filtered by group and by <c>CreatedAt</c>
    /// after <paramref name="since"/>. Access to the group was already validated by the caller
    /// (<c>Notifications.GetGroupEventsUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<ExpenseOccurred>> GetOccurredSinceAsync(
        Guid groupId, DateTimeOffset since, CancellationToken cancellationToken = default)
    {
        return await dbContext.Expenses
            .AsNoTracking()
            .Where(expense => expense.GroupId == groupId && expense.CreatedAt > since)
            .Select(expense => new ExpenseOccurred(
                expense.Id,
                expense.Description,
                expense.TotalAmountCents,
                expense.PayerId,
                expense.CreatedAt))
            .ToListAsync(cancellationToken);
    }

    /// <summary>
    /// The same <c>AsNoTracking</c> + <c>Include</c> of splits read as
    /// <see cref="GetByGroupAsync"/>, but reconstructing <see cref="ExpenseToPersist"/>
    /// (domain Expense + Description + Date) instead of just <see cref="Expense"/> — reuses
    /// <see cref="ExpenseEntityToDomainMapper"/> in full, without duplicating the EF-to-domain
    /// translation. Access to the group was already validated by the caller
    /// (<c>GetGroupUseCase</c>).
    /// </summary>
    public async Task<IReadOnlyList<ExpenseToPersist>> GetDetailedByGroupAsync(
        Guid groupId, CancellationToken cancellationToken = default)
    {
        var entities = await dbContext.Expenses
            .AsNoTracking()
            .Include(expense => expense.Splits)
            .Where(expense => expense.GroupId == groupId)
            .ToListAsync(cancellationToken);

        return entities
            .Select(entity => new ExpenseToPersist(
                ExpenseEntityToDomainMapper.Build(entity), entity.Description, entity.Date))
            .ToList();
    }

    /// <summary>
    /// Loads the entity (with splits, so they can be replaced) filtering by
    /// <paramref name="groupId"/> — <c>false</c> without checking/throwing anything when it doesn't
    /// exist in that group, the caller (<c>EditExpenseUseCase</c>) decides what that means (404).
    /// Replaces the scalar fields and swaps the entire splits collection (never an item-by-item
    /// merge — the same full reconstruction that <c>ExpenseMapper</c>/<c>CreateExpenseUseCase</c>
    /// already require on the domain side), reusing <see cref="ExpenseEntityMapper"/> to build the
    /// new splits instead of duplicating the switch by subtype.
    /// </summary>
    public async Task<bool> UpdateAsync(
        Guid groupId, ExpenseToPersist expense, CancellationToken cancellationToken = default)
    {
        var existingEntity = await dbContext.Expenses
            .Include(e => e.Splits)
            .SingleOrDefaultAsync(e => e.GroupId == groupId && e.Id == expense.Expense.Id, cancellationToken);

        if (existingEntity is null)
        {
            return false;
        }

        UpdateScalarFields(existingEntity, expense);
        ReplaceSplits(existingEntity, expense);

        await dbContext.SaveChangesAsync(cancellationToken);
        return true;
    }

    /// <summary>
    /// Same group-filtered read pattern as <see cref="UpdateAsync"/> — <c>false</c> without
    /// throwing when the expense doesn't exist in that group. The splits are removed via cascade
    /// delete (<c>ExpenseEntityConfiguration</c>), no need to load them here.
    /// </summary>
    public async Task<bool> RemoveAsync(Guid groupId, Guid expenseId, CancellationToken cancellationToken = default)
    {
        var entity = await dbContext.Expenses
            .SingleOrDefaultAsync(e => e.GroupId == groupId && e.Id == expenseId, cancellationToken);

        if (entity is null)
        {
            return false;
        }

        dbContext.Expenses.Remove(entity);
        await dbContext.SaveChangesAsync(cancellationToken);
        return true;
    }

    private static void UpdateScalarFields(ExpenseEntity entity, ExpenseToPersist expense)
    {
        entity.Description = expense.Description;
        entity.Date = expense.Date;
        entity.PayerId = expense.Expense.PayerId.Value;
        entity.TotalAmountCents = expense.Expense.TotalAmount.Cents;
    }

    private void ReplaceSplits(ExpenseEntity entity, ExpenseToPersist expense)
    {
        dbContext.ExpenseSplits.RemoveRange(entity.Splits);
        entity.Splits = ExpenseEntityMapper.Build(expense).Splits;
    }
}
