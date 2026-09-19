using System.IdentityModel.Tokens.Jwt;
using Microsoft.Extensions.Options;
using Rateio.Application.Auth;
using Rateio.Infrastructure.Auth;

namespace Rateio.Infrastructure.Tests.Auth;

/// <summary>
/// Cobre a emissão de tokens de <see cref="JwtIssuer"/> (T11.2). Não depende de ID token real do
/// Google nem de banco — <see cref="IJwtIssuer"/> é puro (recebe um <see cref="Usuario"/> já
/// resolvido, devolve tokens), por isso dá pra testar isolado com um <see cref="RelogioFixo"/>.
/// </summary>
public class JwtIssuerTests
{
    private static readonly DateTimeOffset Agora = new(2026, 9, 19, 12, 0, 0, TimeSpan.Zero);

    private static readonly Usuario UsuarioDeTeste = new(
        Guid.Parse("11111111-1111-1111-1111-111111111111"),
        GoogleSubjectId: "google-sub-123",
        Nome: "Igor Baiocco",
        Email: "igor@example.com");

    [Fact]
    public void Emitir_AccessToken_ExpiraExatamenteNoLimiteConfigurado()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15);

        var tokens = issuer.Emitir(UsuarioDeTeste);

        Assert.Equal(Agora.AddMinutes(15), tokens.AccessTokenExpiraEm);
    }

    [Fact]
    public void Emitir_AccessToken_RespeitaRnf06DeNoMaximo15Minutos()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15);

        var tokens = issuer.Emitir(UsuarioDeTeste);

        var duracao = tokens.AccessTokenExpiraEm - Agora;
        Assert.True(duracao <= TimeSpan.FromMinutes(15), $"Access token expira em {duracao}, viola RNF06.");
    }

    [Fact]
    public void Emitir_AccessToken_CarregaClaimsDoUsuarioSemExpoTokenGoogle()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15);

        var tokens = issuer.Emitir(UsuarioDeTeste);
        var jwt = new JwtSecurityTokenHandler().ReadJwtToken(tokens.AccessToken);

        Assert.Equal(UsuarioDeTeste.Id.ToString(), jwt.Subject);
        Assert.Equal(UsuarioDeTeste.Email, jwt.Claims.Single(c => c.Type == JwtRegisteredClaimNames.Email).Value);
        Assert.Equal("Rateio.Api", jwt.Issuer);
        Assert.Contains("Rateio.App", jwt.Audiences);
    }

    [Fact]
    public void Emitir_RefreshToken_EhDiferenteDoAccessTokenENaoEUmJwt()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15);

        var tokens = issuer.Emitir(UsuarioDeTeste);

        Assert.NotEqual(tokens.AccessToken, tokens.RefreshToken);
        Assert.False(tokens.RefreshToken.Contains('.'), "Refresh token deveria ser opaco, não um JWT.");
    }

    [Fact]
    public void Emitir_RefreshToken_ExpiraDeAcordoComConfiguracao()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15, refreshTokenDias: 30);

        var tokens = issuer.Emitir(UsuarioDeTeste);

        Assert.Equal(Agora.AddDays(30), tokens.RefreshTokenExpiraEm);
    }

    [Fact]
    public void Emitir_ChamadasSucessivas_GeramRefreshTokensDistintos()
    {
        var issuer = CriarIssuer(accessTokenMinutos: 15);

        var primeiro = issuer.Emitir(UsuarioDeTeste);
        var segundo = issuer.Emitir(UsuarioDeTeste);

        Assert.NotEqual(primeiro.RefreshToken, segundo.RefreshToken);
    }

    private static JwtIssuer CriarIssuer(int accessTokenMinutos, int refreshTokenDias = 30)
    {
        var opcoes = Options.Create(new JwtOptions
        {
            SigningKey = "chave-de-teste-com-tamanho-suficiente-para-hmac-sha256",
            Issuer = "Rateio.Api",
            Audience = "Rateio.App",
            AccessTokenMinutos = accessTokenMinutos,
            RefreshTokenDias = refreshTokenDias,
        });

        return new JwtIssuer(opcoes, new RelogioFixo(Agora));
    }
}
