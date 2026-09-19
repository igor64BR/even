namespace Rateio.Api.Contracts;

/// <summary>Corpo de <c>POST /auth/google</c>.</summary>
public sealed record GoogleLoginRequest(string IdToken);
