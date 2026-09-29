using Microsoft.AspNetCore.Mvc;
using Tally.Api.Contracts;
using Tally.Application.Auth;

namespace Tally.Api.Controllers;

/// <summary>
/// T11: exchanges a Google ID token for our own JWT. No token validation/issuance logic here — the
/// controller only receives the request, calls <see cref="AuthenticateWithGoogleUseCase"/>, and
/// translates the result (or failure) to HTTP.
/// </summary>
[ApiController]
[Route("auth")]
public class AuthController(
    AuthenticateWithGoogleUseCase authenticateWithGoogle,
    RevokeSessionUseCase revokeSession) : ControllerBase
{
    [HttpPost("google")]
    public async Task<ActionResult<GoogleLoginResponse>> Google(
        [FromBody] GoogleLoginRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.IdToken))
        {
            return Problem(
                title: "idToken is required.",
                statusCode: StatusCodes.Status400BadRequest);
        }

        try
        {
            var result = await authenticateWithGoogle.ExecuteAsync(request.IdToken, cancellationToken);

            return Ok(GoogleLoginResponse.From(result));
        }
        catch (InvalidGoogleTokenException error)
        {
            // The exception message already comes without the token (see GoogleTokenValidator) —
            // safe to return to the client and to let Serilog log it via UseSerilogRequestLogging.
            return Problem(
                title: "Invalid Google ID token.",
                detail: error.Message,
                statusCode: StatusCodes.Status401Unauthorized);
        }
    }

    /// <summary>
    /// T14.1: revokes the session for the given refresh token (see <see cref="LogoutRequest"/> for
    /// why it comes in the body, not the Authorization header). Always 204, even if the token was
    /// already revoked or never existed — <see cref="RevokeSessionUseCase"/> is deliberately
    /// idempotent, so as not to expose to the client whether a given token ever existed.
    /// </summary>
    [HttpPost("logout")]
    public async Task<IActionResult> Logout(
        [FromBody] LogoutRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.RefreshToken))
        {
            return Problem(
                title: "refreshToken is required.",
                statusCode: StatusCodes.Status400BadRequest);
        }

        await revokeSession.ExecuteAsync(request.RefreshToken, cancellationToken);

        return NoContent();
    }
}
