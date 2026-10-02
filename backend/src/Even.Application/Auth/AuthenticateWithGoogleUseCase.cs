namespace Even.Application.Auth;

/// <summary>
/// Orchestrates Google login: validates the ID token, makes sure a matching local user
/// exists, issues the application's token pair, and persists the refresh token so it can be
/// revoked later. This is where — not in the controller — the step sequence and the "new vs.
/// existing user" rules live, keeping the controller a pure HTTP orchestrator.
/// </summary>
public sealed class AuthenticateWithGoogleUseCase(
    IGoogleTokenValidator googleValidator,
    IUserRepository users,
    IJwtIssuer jwtIssuer,
    IRefreshTokenRepository refreshTokens)
{
    public async Task<AuthenticationResult> ExecuteAsync(string idToken, CancellationToken cancellationToken = default)
    {
        var googleData = await googleValidator.ValidateAsync(idToken, cancellationToken);
        var user = await GetOrCreateUserAsync(googleData, cancellationToken);
        var tokens = jwtIssuer.Issue(user);

        await refreshTokens.SaveAsync(user.Id, tokens.RefreshToken, tokens.RefreshTokenExpiresAt, cancellationToken);

        return new AuthenticationResult(tokens.AccessToken, tokens.RefreshToken, user);
    }

    private async Task<User> GetOrCreateUserAsync(GoogleUserInfo googleData, CancellationToken cancellationToken)
    {
        var existingUser = await users.GetByGoogleSubjectIdAsync(googleData.GoogleSubjectId, cancellationToken);

        return existingUser ?? await users.CreateAsync(googleData, cancellationToken);
    }
}
