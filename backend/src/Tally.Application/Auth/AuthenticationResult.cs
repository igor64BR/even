namespace Tally.Application.Auth;

/// <summary>
/// Result of a successful login: the tokens the app should use from then on and the user's data
/// for immediate display (avoids the app needing a second call just to know who logged in).
/// </summary>
public sealed record AuthenticationResult(string AccessToken, string RefreshToken, User User);
