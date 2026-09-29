using Moq;
using Tally.Application.Auth;

namespace Tally.Application.Tests.Auth;

/// <summary>
/// Covers <see cref="RevokeSessionUseCase"/> (T14.1) with a mocked
/// <see cref="IRefreshTokenRepository"/> — same style as
/// <see cref="AuthenticateWithGoogleUseCaseTests"/>, no real database. Idempotency itself (an
/// already-revoked or unknown token is not an error) is the responsibility of the
/// <see cref="IRefreshTokenRepository"/> implementation (see RefreshTokenRepository), not of the
/// use case, which just forwards the call.
/// </summary>
public class RevokeSessionUseCaseTests
{
    private readonly Mock<IRefreshTokenRepository> _refreshTokens = new();

    private RevokeSessionUseCase CreateUseCase() => new(_refreshTokens.Object);

    [Fact]
    public async Task ExecuteAsync_WithValidRefreshToken_CallsRevokeAsyncWithTheSameToken()
    {
        var useCase = CreateUseCase();

        await useCase.ExecuteAsync("user-refresh-token");

        _refreshTokens.Verify(
            r => r.RevokeAsync("user-refresh-token", It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_PropagatesCancellationTokenToTheRepository()
    {
        using var cancellationTokenSource = new CancellationTokenSource();
        var useCase = CreateUseCase();

        await useCase.ExecuteAsync("user-refresh-token", cancellationTokenSource.Token);

        _refreshTokens.Verify(
            r => r.RevokeAsync("user-refresh-token", cancellationTokenSource.Token),
            Times.Once);
    }
}
