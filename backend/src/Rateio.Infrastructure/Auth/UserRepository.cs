using Microsoft.EntityFrameworkCore;
using Rateio.Application.Auth;
using Rateio.Infrastructure.Persistence;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementation of <see cref="IUserRepository"/> via EF Core / <see cref="AppDbContext"/>.
/// </summary>
public sealed class UserRepository(AppDbContext dbContext) : IUserRepository
{
    public async Task<User?> GetByGoogleSubjectIdAsync(
        string googleSubjectId,
        CancellationToken cancellationToken = default)
    {
        var entity = await dbContext.Users
            .AsNoTracking()
            .SingleOrDefaultAsync(user => user.GoogleSubjectId == googleSubjectId, cancellationToken);

        return entity is null ? null : ToApplicationModel(entity);
    }

    public async Task<User> CreateAsync(GoogleUserInfo googleData, CancellationToken cancellationToken = default)
    {
        var entity = new UserEntity
        {
            Id = Guid.NewGuid(),
            GoogleSubjectId = googleData.GoogleSubjectId,
            Name = googleData.Name,
            Email = googleData.Email,
            CreatedAt = DateTimeOffset.UtcNow,
        };

        dbContext.Users.Add(entity);
        await dbContext.SaveChangesAsync(cancellationToken);

        return ToApplicationModel(entity);
    }

    private static User ToApplicationModel(UserEntity entity) =>
        new(entity.Id, entity.GoogleSubjectId, entity.Name, entity.Email);
}
