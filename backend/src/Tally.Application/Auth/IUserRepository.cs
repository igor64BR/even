namespace Tally.Application.Auth;

/// <summary>
/// Persistence of users authenticated via Google. Concrete implementation (EF Core) in
/// Tally.Infrastructure.
/// </summary>
public interface IUserRepository
{
    Task<User?> GetByGoogleSubjectIdAsync(string googleSubjectId, CancellationToken cancellationToken = default);

    Task<User> CreateAsync(GoogleUserInfo googleData, CancellationToken cancellationToken = default);
}
