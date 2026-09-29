using Google.Apis.Auth;
using Microsoft.Extensions.Options;
using Rateio.Application.Auth;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Implementation of <see cref="IGoogleTokenValidator"/> via Google.Apis.Auth
/// (<see cref="GoogleJsonWebSignature.ValidateAsync"/>): checks signature, issuer, and audience
/// against Google's servers. Never logs the received token nor the decoded payload.
/// </summary>
public sealed class GoogleTokenValidator(IOptions<GoogleAuthOptions> options) : IGoogleTokenValidator
{
    private readonly GoogleAuthOptions _options = options.Value;

    public async Task<GoogleUserInfo> ValidateAsync(string idToken, CancellationToken cancellationToken = default)
    {
        var payload = await ValidateSignatureAndAudienceAsync(idToken);

        EnsureEmailVerified(payload);

        return new GoogleUserInfo(payload.Subject, NameOrEmail(payload), payload.Email);
    }

    private async Task<GoogleJsonWebSignature.Payload> ValidateSignatureAndAudienceAsync(string idToken)
    {
        var validationSettings = new GoogleJsonWebSignature.ValidationSettings
        {
            Audience = [_options.ClientId],
        };

        try
        {
            return await GoogleJsonWebSignature.ValidateAsync(idToken, validationSettings);
        }
        catch (InvalidJwtException error)
        {
            throw new InvalidGoogleTokenException("Google ID token rejected: " + error.Message);
        }
    }

    private static void EnsureEmailVerified(GoogleJsonWebSignature.Payload payload)
    {
        if (!payload.EmailVerified)
        {
            throw new InvalidGoogleTokenException("Google ID token has an unverified e-mail.");
        }
    }

    private static string NameOrEmail(GoogleJsonWebSignature.Payload payload) =>
        string.IsNullOrWhiteSpace(payload.Name) ? payload.Email : payload.Name;
}
