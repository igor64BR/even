namespace Even.Infrastructure.Auth;

/// <summary>
/// Config for issuing the application's own JWT. <see cref="SigningKey"/> in
/// <c>appsettings.Development.json</c> is a local-development-only secret — in production it must
/// come from a secure source (user-secrets/environment variable/vault), never committed.
/// </summary>
public sealed class JwtOptions
{
    public const string ConfigurationSection = "Jwt";

    public required string SigningKey { get; init; }

    public required string Issuer { get; init; }

    public required string Audience { get; init; }

    /// <summary>Access token expires in &lt;= 15 min.</summary>
    public required int AccessTokenMinutes { get; init; }

    public required int RefreshTokenDays { get; init; }
}
