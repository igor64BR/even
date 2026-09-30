namespace Tally.Application.Auth;

/// <summary>
/// Orchestrates logout: revokes the session associated with a refresh token. Exists as its
/// own use case — instead of the controller calling <see cref="IRefreshTokenRepository"/>
/// directly — to keep the same pattern as <see cref="AuthenticateWithGoogleUseCase"/> (the
/// controller only translates HTTP, the business rule lives here) and to give a single extension
/// point if logout ever needs to do more than revoke (e.g. notifying other sessions via SignalR).
/// </summary>
public sealed class RevokeSessionUseCase(IRefreshTokenRepository refreshTokens)
{
    public Task ExecuteAsync(string refreshToken, CancellationToken cancellationToken = default) =>
        refreshTokens.RevokeAsync(refreshToken, cancellationToken);
}
