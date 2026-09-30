using Moq;
using Tally.Application.Auth;

namespace Tally.Application.Tests.Auth;

/// <summary>
/// Covers the orchestration of <see cref="AuthenticateWithGoogleUseCase"/> entirely with
/// doubles — no test here needs a real Google ID token or a database. The "invalid/expired token"
/// path is simulated by making the mocked <see cref="IGoogleTokenValidator"/> throw
/// <see cref="InvalidGoogleTokenException"/>, exactly like the real implementation would.
/// </summary>
public class AuthenticateWithGoogleUseCaseTests
{
    private static readonly GoogleUserInfo GoogleData = new("google-sub-123", "Igor Baiocco", "igor@example.com");

    private readonly Mock<IGoogleTokenValidator> _googleValidator = new();
    private readonly Mock<IUserRepository> _users = new();
    private readonly Mock<IJwtIssuer> _jwtIssuer = new();
    private readonly Mock<IRefreshTokenRepository> _refreshTokens = new();

    private AuthenticateWithGoogleUseCase CreateUseCase() => new(
        _googleValidator.Object,
        _users.Object,
        _jwtIssuer.Object,
        _refreshTokens.Object);

    [Fact]
    public async Task ExecuteAsync_WithInvalidOrExpiredToken_PropagatesInvalidGoogleTokenExceptionWithoutCallingAnythingElse()
    {
        _googleValidator
            .Setup(v => v.ValidateAsync("invalid-id-token", It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidGoogleTokenException("Google ID token rejected: token expired."));

        var useCase = CreateUseCase();

        await Assert.ThrowsAsync<InvalidGoogleTokenException>(
            () => useCase.ExecuteAsync("invalid-id-token"));

        _users.Verify(u => u.GetByGoogleSubjectIdAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
        _jwtIssuer.Verify(j => j.Issue(It.IsAny<User>()), Times.Never);
        _refreshTokens.Verify(
            r => r.SaveAsync(It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecuteAsync_NewUser_CreatesUserIssuesAndPersistsRefreshToken()
    {
        var createdUser = new User(Guid.NewGuid(), GoogleData.GoogleSubjectId, GoogleData.Name, GoogleData.Email);
        var tokens = new TokenPair(
            "access-token",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "refresh-token",
            DateTimeOffset.UtcNow.AddDays(30));

        _googleValidator.Setup(v => v.ValidateAsync("valid-id-token", It.IsAny<CancellationToken>()))
            .ReturnsAsync(GoogleData);
        _users.Setup(u => u.GetByGoogleSubjectIdAsync(GoogleData.GoogleSubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((User?)null);
        _users.Setup(u => u.CreateAsync(GoogleData, It.IsAny<CancellationToken>()))
            .ReturnsAsync(createdUser);
        _jwtIssuer.Setup(j => j.Issue(createdUser)).Returns(tokens);

        var useCase = CreateUseCase();

        var result = await useCase.ExecuteAsync("valid-id-token");

        Assert.Equal(tokens.AccessToken, result.AccessToken);
        Assert.Equal(tokens.RefreshToken, result.RefreshToken);
        Assert.Equal(createdUser, result.User);
        _users.Verify(u => u.CreateAsync(GoogleData, It.IsAny<CancellationToken>()), Times.Once);
        _refreshTokens.Verify(
            r => r.SaveAsync(createdUser.Id, tokens.RefreshToken, tokens.RefreshTokenExpiresAt, It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecuteAsync_ExistingUser_DoesNotCreateUserAgain()
    {
        var existingUser = new User(Guid.NewGuid(), GoogleData.GoogleSubjectId, GoogleData.Name, GoogleData.Email);
        var tokens = new TokenPair(
            "access-token",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "refresh-token",
            DateTimeOffset.UtcNow.AddDays(30));

        _googleValidator.Setup(v => v.ValidateAsync("valid-id-token", It.IsAny<CancellationToken>()))
            .ReturnsAsync(GoogleData);
        _users.Setup(u => u.GetByGoogleSubjectIdAsync(GoogleData.GoogleSubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(existingUser);
        _jwtIssuer.Setup(j => j.Issue(existingUser)).Returns(tokens);

        var useCase = CreateUseCase();

        await useCase.ExecuteAsync("valid-id-token");

        _users.Verify(u => u.CreateAsync(It.IsAny<GoogleUserInfo>(), It.IsAny<CancellationToken>()), Times.Never);
    }
}
