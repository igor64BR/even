using Microsoft.EntityFrameworkCore;
using Rateio.Application.Expenses;
using Rateio.Application.Groups;
using Rateio.Domain;
using Rateio.Infrastructure.Expenses;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Groups;

/// <summary>
/// Implementation of <see cref="IGroupRepository"/> via EF Core / <see cref="AppDbContext"/>
/// (T18.1, T23.2). Domain-to-EF mapping in small, named methods, mirroring
/// <c>SyncedGroupBuilder</c> (Application) on the opposite side of the translation. Doesn't
/// manually set <c>GroupId</c>/<c>ExpenseId</c> on child entities — EF resolves that via
/// relationship fixup from the navigation collections, the same way the configurations in
/// <c>Persistence/Configurations</c> already declare.
/// </summary>
public sealed class GroupRepository(AppDbContext dbContext) : IGroupRepository
{
    public async Task<Guid> SyncAsync(GroupToSync group, CancellationToken cancellationToken = default)
    {
        var entity = BuildGroupEntity(group);

        dbContext.Groups.Add(entity);
        await dbContext.SaveChangesAsync(cancellationToken);

        return entity.Id;
    }

    /// <summary>
    /// T23.2: minimal read (no participants/expenses) just to validate existence + RNF07 before
    /// adding a new expense — <c>AsNoTracking</c> because it's read-only.
    /// </summary>
    public Task<GroupAccess?> GetAccessAsync(Guid groupId, CancellationToken cancellationToken = default) =>
        dbContext.Groups
            .AsNoTracking()
            .Where(group => group.Id == groupId)
            .Select(group => new GroupAccess(group.Id, group.OwnerUserId))
            .FirstOrDefaultAsync(cancellationToken);

    /// <summary>
    /// T21.2: minimal read (name/category/participants, no expenses) for
    /// <c>JoinGroupViaInviteUseCase</c> to reconstruct the domain aggregate before calling
    /// <c>Group.AddParticipant</c> — <c>AsNoTracking</c> because it's read-only, the actual insert
    /// happens in <see cref="AddParticipantAsync"/>.
    /// </summary>
    public async Task<GroupEntryInfo?> GetForEntryAsync(Guid groupId, CancellationToken cancellationToken = default)
    {
        var entity = await dbContext.Groups
            .AsNoTracking()
            .Include(group => group.Participants)
            .SingleOrDefaultAsync(group => group.Id == groupId, cancellationToken);

        return entity is null ? null : ToGroupEntryInfo(entity);
    }

    /// <summary>
    /// T21.2: inserts the participant directly via the FK (<c>GroupId</c>), without reloading the
    /// whole <see cref="GroupEntity"/> — same pattern as
    /// <c>Rateio.Infrastructure.Expenses.ExpenseRepository.AddAsync</c> (T23.2).
    /// </summary>
    public Task AddParticipantAsync(Guid groupId, Participant participant, CancellationToken cancellationToken = default)
    {
        dbContext.Participants.Add(new ParticipantEntity
        {
            Id = participant.Id.Value,
            GroupId = groupId,
            Name = participant.Name.Value,
            IsGuest = participant.IsGuest,
        });

        return dbContext.SaveChangesAsync(cancellationToken);
    }

    private static GroupEntryInfo ToGroupEntryInfo(GroupEntity entity) => new(
        GroupName.Create(entity.Name),
        entity.Category,
        entity.Participants.Select(ToDomainParticipant).ToList());

    private static Participant ToDomainParticipant(ParticipantEntity entity)
    {
        var id = new ParticipantId(entity.Id);
        var name = ParticipantName.Create(entity.Name);

        return entity.IsGuest
            ? Participant.Guest(id, name)
            : Participant.Authenticated(id, name);
    }

    private static GroupEntity BuildGroupEntity(GroupToSync groupToSync) => new()
    {
        Id = groupToSync.Group.Id,
        Name = groupToSync.Group.Name.Value,
        Category = groupToSync.Group.Category,
        Synced = groupToSync.Group.Synced,
        OwnerUserId = groupToSync.OwnerUserId,
        CreatedAt = DateTimeOffset.UtcNow,
        Participants = BuildParticipantEntities(groupToSync.Group.Participants),
        Expenses = BuildExpenseEntities(groupToSync.Expenses),
    };

    private static List<ParticipantEntity> BuildParticipantEntities(
        IReadOnlyList<Participant> participants) =>
        participants.Select(BuildParticipantEntity).ToList();

    private static ParticipantEntity BuildParticipantEntity(Participant participant) => new()
    {
        Id = participant.Id.Value,
        Name = participant.Name.Value,
        IsGuest = participant.IsGuest,
    };

    // T23 extracted building the ExpenseEntity itself (including splits) into
    // Rateio.Infrastructure.Expenses.ExpenseEntityMapper — reused here and by the new
    // ExpenseRepository, instead of duplicated.
    private static List<ExpenseEntity> BuildExpenseEntities(IReadOnlyList<ExpenseToPersist> expenses) =>
        expenses.Select(ExpenseEntityMapper.Build).ToList();
}
