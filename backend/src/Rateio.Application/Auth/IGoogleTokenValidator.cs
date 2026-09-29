namespace Rateio.Application.Auth;

/// <summary>
/// Abstracts validation of the Google ID token (signature, issuer, and audience). The concrete
/// implementation (Google.Apis.Auth) lives in Rateio.Infrastructure — Application never
/// references the Google library, only this interface (Dependency Inversion: makes it easy to
/// test the rest of the flow without a real token and to swap providers without touching use
/// cases or the controller).
/// </summary>
public interface IGoogleTokenValidator
{
    /// <summary>
    /// Validates the ID token and extracts the user's identity.
    /// </summary>
    /// <exception cref="InvalidGoogleTokenException">
    /// Token with invalid signature, wrong audience/issuer, expired, or unverified e-mail.
    /// </exception>
    Task<GoogleUserInfo> ValidateAsync(string idToken, CancellationToken cancellationToken = default);
}
