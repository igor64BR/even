namespace Even.Infrastructure.Auth;

/// <summary>
/// Config for validating the Google ID token. <see cref="ClientId"/> is the expected audience —
/// the project's OAuth client id in the Google Cloud Console. In
/// <c>appsettings.Development.json</c> today it's a placeholder; it needs to be swapped for the
/// real client id as soon as the Google Cloud project exists.
/// </summary>
public sealed class GoogleAuthOptions
{
    public const string ConfigurationSection = "GoogleAuth";

    public required string ClientId { get; init; }
}
