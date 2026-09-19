using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Rateio.Application.Auth;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementação de <see cref="IJwtIssuer"/>: access token é um JWT assinado (HMAC-SHA256),
/// refresh token é um valor opaco aleatório (não é um JWT — não precisa ser decodificável, só
/// imprevisível). Recebe <see cref="TimeProvider"/> em vez de usar <see cref="DateTimeOffset.UtcNow"/>
/// direto, pra dar pra testar expiração sem depender do relógio real.
/// </summary>
public sealed class JwtIssuer(IOptions<JwtOptions> opcoes, TimeProvider relogio) : IJwtIssuer
{
    private const int TamanhoRefreshTokenEmBytes = 32;

    private readonly JwtOptions _opcoes = opcoes.Value;

    public ParDeTokens Emitir(Usuario usuario)
    {
        var agora = relogio.GetUtcNow();
        var accessTokenExpiraEm = agora.AddMinutes(_opcoes.AccessTokenMinutos);
        var refreshTokenExpiraEm = agora.AddDays(_opcoes.RefreshTokenDias);

        var accessToken = GerarAccessToken(usuario, agora, accessTokenExpiraEm);
        var refreshToken = GerarRefreshTokenOpaco();

        return new ParDeTokens(accessToken, accessTokenExpiraEm, refreshToken, refreshTokenExpiraEm);
    }

    private string GerarAccessToken(Usuario usuario, DateTimeOffset agora, DateTimeOffset expiraEm)
    {
        var credenciais = new SigningCredentials(ChaveDeAssinatura(), SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _opcoes.Issuer,
            audience: _opcoes.Audience,
            claims: ClaimsDoUsuario(usuario),
            notBefore: agora.UtcDateTime,
            expires: expiraEm.UtcDateTime,
            signingCredentials: credenciais);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private static List<Claim> ClaimsDoUsuario(Usuario usuario) =>
    [
        new(JwtRegisteredClaimNames.Sub, usuario.Id.ToString()),
        new(JwtRegisteredClaimNames.Email, usuario.Email),
        new(JwtRegisteredClaimNames.Name, usuario.Nome),
        new("google_sub", usuario.GoogleSubjectId),
    ];

    private SymmetricSecurityKey ChaveDeAssinatura() =>
        new(System.Text.Encoding.UTF8.GetBytes(_opcoes.SigningKey));

    private static string GerarRefreshTokenOpaco() =>
        Base64UrlEncoder.Encode(RandomNumberGenerator.GetBytes(TamanhoRefreshTokenEmBytes));
}
