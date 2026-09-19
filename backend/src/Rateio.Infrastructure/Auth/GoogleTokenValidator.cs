using Google.Apis.Auth;
using Microsoft.Extensions.Options;
using Rateio.Application.Auth;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementação de <see cref="IGoogleTokenValidator"/> via Google.Apis.Auth
/// (<see cref="GoogleJsonWebSignature.ValidateAsync"/>): verifica assinatura, emissor e audience
/// contra os servidores do Google. Nunca loga o token recebido nem o payload decodificado.
/// </summary>
public sealed class GoogleTokenValidator(IOptions<GoogleAuthOptions> opcoes) : IGoogleTokenValidator
{
    private readonly GoogleAuthOptions _opcoes = opcoes.Value;

    public async Task<GoogleUserInfo> ValidarAsync(string idToken, CancellationToken cancellationToken = default)
    {
        var payload = await ValidarAssinaturaEAudienceAsync(idToken);

        GarantirEmailVerificado(payload);

        return new GoogleUserInfo(payload.Subject, NomeOuEmail(payload), payload.Email);
    }

    private async Task<GoogleJsonWebSignature.Payload> ValidarAssinaturaEAudienceAsync(string idToken)
    {
        var configuracaoValidacao = new GoogleJsonWebSignature.ValidationSettings
        {
            Audience = [_opcoes.ClientId],
        };

        try
        {
            return await GoogleJsonWebSignature.ValidateAsync(idToken, configuracaoValidacao);
        }
        catch (InvalidJwtException erro)
        {
            throw new GoogleTokenInvalidoException("ID token do Google rejeitado: " + erro.Message);
        }
    }

    private static void GarantirEmailVerificado(GoogleJsonWebSignature.Payload payload)
    {
        if (!payload.EmailVerified)
        {
            throw new GoogleTokenInvalidoException("ID token do Google tem e-mail não verificado.");
        }
    }

    private static string NomeOuEmail(GoogleJsonWebSignature.Payload payload) =>
        string.IsNullOrWhiteSpace(payload.Name) ? payload.Email : payload.Name;
}
