using Microsoft.EntityFrameworkCore;
using Even.Application.Groups;
using Even.Infrastructure.Persistence;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Groups;

/// <summary>
/// Implementation of <see cref="IInviteCodeRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// </summary>
public sealed class InviteCodeRepository(AppDbContext dbContext) : IInviteCodeRepository
{
    /// <summary>
    /// Upsert by group (see <c>InviteCode</c> — "one active code per group"): removes any previous
    /// code from the same <see cref="InviteCode.GroupId"/> before inserting the new one, in a
    /// single implicit transaction from <see cref="DbContext.SaveChangesAsync(CancellationToken)"/>.
    /// </summary>
    public async Task SaveAsync(InviteCode code, CancellationToken cancellationToken = default)
    {
        var previousCodes = await dbContext.InviteCodes
            .Where(entity => entity.GroupId == code.GroupId)
            .ToListAsync(cancellationToken);

        dbContext.InviteCodes.RemoveRange(previousCodes);
        dbContext.InviteCodes.Add(BuildEntity(code));

        await dbContext.SaveChangesAsync(cancellationToken);
    }

    public async Task<InviteCode?> GetByCodeAsync(string code, CancellationToken cancellationToken = default)
    {
        var entity = await dbContext.InviteCodes
            .AsNoTracking()
            .SingleOrDefaultAsync(c => c.Code == code, cancellationToken);

        return entity is null ? null : ToApplicationModel(entity);
    }

    private static InviteCodeEntity BuildEntity(InviteCode code) => new()
    {
        Code = code.Value,
        GroupId = code.GroupId,
        CreatedAt = code.CreatedAt,
        ExpiresAt = code.ExpiresAt,
    };

    private static InviteCode ToApplicationModel(InviteCodeEntity entity) =>
        new(entity.Code, entity.GroupId, entity.CreatedAt, entity.ExpiresAt);
}
