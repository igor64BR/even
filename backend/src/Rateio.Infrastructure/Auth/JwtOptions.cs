namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Config de emissão do JWT próprio da aplicação. <see cref="SigningKey"/> em
/// <c>appsettings.Development.json</c> é um segredo só de desenvolvimento local — em produção deve
/// vir de uma fonte segura (user-secrets/variável de ambiente/cofre), nunca commitado.
/// </summary>
public sealed class JwtOptions
{
    public const string SecaoConfiguracao = "Jwt";

    public required string SigningKey { get; init; }

    public required string Issuer { get; init; }

    public required string Audience { get; init; }

    /// <summary>RNF06: access token expira em ≤ 15 min.</summary>
    public required int AccessTokenMinutos { get; init; }

    public required int RefreshTokenDias { get; init; }
}
