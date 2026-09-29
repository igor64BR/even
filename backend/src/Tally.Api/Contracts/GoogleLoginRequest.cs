namespace Tally.Api.Contracts;

/// <summary>Body of <c>POST /auth/google</c>.</summary>
public sealed record GoogleLoginRequest(string IdToken);
