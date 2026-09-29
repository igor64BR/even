using System.IdentityModel.Tokens.Jwt;
using Microsoft.Extensions.Options;
using Rateio.Application.Auth;
using Rateio.Infrastructure.Auth;

namespace Rateio.Infrastructure.Tests.Auth;

/// <summary>
/// Covers <see cref="JwtIssuer"/>'s token issuance (T11.2). Doesn't depend on a real Google ID
/// token or a database — <see cref="IJwtIssuer"/> is pure (takes an already-resolved
/// <see cref="User"/>, returns tokens), so it can be tested in isolation with a
/// <see cref="FixedClock"/>.
/// </summary>
public class JwtIssuerTests
{
    private static readonly DateTimeOffset Now = new(2026, 9, 19, 12, 0, 0, TimeSpan.Zero);

    private static readonly User TestUser = new(
        Guid.Parse("11111111-1111-1111-1111-111111111111"),
        GoogleSubjectId: "google-sub-123",
        Name: "Igor Baiocco",
        Email: "igor@example.com");

    [Fact]
    public void Issue_AccessToken_ExpiresExactlyAtTheConfiguredLimit()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15);

        var tokens = issuer.Issue(TestUser);

        Assert.Equal(Now.AddMinutes(15), tokens.AccessTokenExpiresAt);
    }

    [Fact]
    public void Issue_AccessToken_RespectsRnf06OfAtMost15Minutes()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15);

        var tokens = issuer.Issue(TestUser);

        var duration = tokens.AccessTokenExpiresAt - Now;
        Assert.True(duration <= TimeSpan.FromMinutes(15), $"Access token expires in {duration}, violating RNF06.");
    }

    [Fact]
    public void Issue_AccessToken_CarriesUserClaimsWithoutExposingTheGoogleToken()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15);

        var tokens = issuer.Issue(TestUser);
        var jwt = new JwtSecurityTokenHandler().ReadJwtToken(tokens.AccessToken);

        Assert.Equal(TestUser.Id.ToString(), jwt.Subject);
        Assert.Equal(TestUser.Email, jwt.Claims.Single(c => c.Type == JwtRegisteredClaimNames.Email).Value);
        Assert.Equal("Rateio.Api", jwt.Issuer);
        Assert.Contains("Rateio.App", jwt.Audiences);
    }

    [Fact]
    public void Issue_RefreshToken_IsDifferentFromTheAccessTokenAndIsNotAJwt()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15);

        var tokens = issuer.Issue(TestUser);

        Assert.NotEqual(tokens.AccessToken, tokens.RefreshToken);
        Assert.False(tokens.RefreshToken.Contains('.'), "Refresh token should be opaque, not a JWT.");
    }

    [Fact]
    public void Issue_RefreshToken_ExpiresAccordingToConfiguration()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15, refreshTokenDays: 30);

        var tokens = issuer.Issue(TestUser);

        Assert.Equal(Now.AddDays(30), tokens.RefreshTokenExpiresAt);
    }

    [Fact]
    public void Issue_SuccessiveCalls_GenerateDistinctRefreshTokens()
    {
        var issuer = CreateIssuer(accessTokenMinutes: 15);

        var first = issuer.Issue(TestUser);
        var second = issuer.Issue(TestUser);

        Assert.NotEqual(first.RefreshToken, second.RefreshToken);
    }

    private static JwtIssuer CreateIssuer(int accessTokenMinutes, int refreshTokenDays = 30)
    {
        var options = Options.Create(new JwtOptions
        {
            SigningKey = "test-signing-key-long-enough-for-hmac-sha256",
            Issuer = "Rateio.Api",
            Audience = "Rateio.App",
            AccessTokenMinutes = accessTokenMinutes,
            RefreshTokenDays = refreshTokenDays,
        });

        return new JwtIssuer(options, new FixedClock(Now));
    }
}
