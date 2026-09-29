namespace Rateio.Application.Auth;

/// <summary>
/// Google ID token rejected — invalid signature, wrong audience, wrong issuer, expired, or
/// e-mail not verified by Google. The message must never include the token itself.
/// </summary>
public sealed class InvalidGoogleTokenException(string message) : Exception(message);
