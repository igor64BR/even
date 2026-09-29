using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Rateio.Application.Auth;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementation of <see cref="IJwtIssuer"/>: the access token is a signed JWT (HMAC-SHA256),
/// the refresh token is a random opaque value (not a JWT — it doesn't need to be decodable, just
/// unpredictable). Takes a <see cref="TimeProvider"/> instead of using
/// <see cref="DateTimeOffset.UtcNow"/> directly, so expiration can be tested without depending on
/// the real clock.
/// </summary>
public sealed class JwtIssuer(IOptions<JwtOptions> options, TimeProvider clock) : IJwtIssuer
{
    private const int RefreshTokenSizeInBytes = 32;

    private readonly JwtOptions _options = options.Value;

    public TokenPair Issue(User user)
    {
        var now = clock.GetUtcNow();
        var accessTokenExpiresAt = now.AddMinutes(_options.AccessTokenMinutes);
        var refreshTokenExpiresAt = now.AddDays(_options.RefreshTokenDays);

        var accessToken = GenerateAccessToken(user, now, accessTokenExpiresAt);
        var refreshToken = GenerateOpaqueRefreshToken();

        return new TokenPair(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
    }

    private string GenerateAccessToken(User user, DateTimeOffset now, DateTimeOffset expiresAt)
    {
        var credentials = new SigningCredentials(SigningKey(), SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _options.Issuer,
            audience: _options.Audience,
            claims: UserClaims(user),
            notBefore: now.UtcDateTime,
            expires: expiresAt.UtcDateTime,
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private static List<Claim> UserClaims(User user) =>
    [
        new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
        new(JwtRegisteredClaimNames.Email, user.Email),
        new(JwtRegisteredClaimNames.Name, user.Name),
        new("google_sub", user.GoogleSubjectId),
    ];

    private SymmetricSecurityKey SigningKey() =>
        new(System.Text.Encoding.UTF8.GetBytes(_options.SigningKey));

    private static string GenerateOpaqueRefreshToken() =>
        Base64UrlEncoder.Encode(RandomNumberGenerator.GetBytes(RefreshTokenSizeInBytes));
}
