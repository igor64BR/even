using Tally.Application.Auth;

namespace Tally.Api.Contracts;

/// <summary>Success response of <c>POST /auth/google</c>.</summary>
public sealed record GoogleLoginResponse(string AccessToken, string RefreshToken, UserResponse User)
{
    public static GoogleLoginResponse From(AuthenticationResult result) => new(
        result.AccessToken,
        result.RefreshToken,
        new UserResponse(result.User.Name, result.User.Email));
}

public sealed record UserResponse(string Name, string Email);
