using Microsoft.AspNetCore.Mvc;
using Rateio.Api.Contracts;
using Rateio.Application.Auth;

namespace Rateio.Api.Controllers;

/// <summary>
/// T11: troca de ID token do Google por um JWT próprio. Sem lógica de validação/emissão de token
/// aqui — o controller só recebe a requisição, chama <see cref="AutenticarComGoogleUseCase"/> e
/// traduz o resultado (ou a falha) pra HTTP.
/// </summary>
[ApiController]
[Route("auth")]
public class AuthController(
    AutenticarComGoogleUseCase autenticarComGoogle,
    RevogarSessaoUseCase revogarSessao) : ControllerBase
{
    [HttpPost("google")]
    public async Task<ActionResult<GoogleLoginResponse>> Google(
        [FromBody] GoogleLoginRequest requisicao,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(requisicao.IdToken))
        {
            return Problem(
                title: "idToken é obrigatório.",
                statusCode: StatusCodes.Status400BadRequest);
        }

        try
        {
            var resultado = await autenticarComGoogle.ExecutarAsync(requisicao.IdToken, cancellationToken);

            return Ok(GoogleLoginResponse.De(resultado));
        }
        catch (GoogleTokenInvalidoException erro)
        {
            // Mensagem da exceção já vem sem o token (ver GoogleTokenValidator) — segura de
            // devolver ao cliente e de deixar o Serilog logar via UseSerilogRequestLogging.
            return Problem(
                title: "ID token do Google inválido.",
                detail: erro.Message,
                statusCode: StatusCodes.Status401Unauthorized);
        }
    }

    /// <summary>
    /// T14.1: revoga a sessão do refresh token informado (ver <see cref="LogoutRequest"/> pra
    /// justificativa de por que ele vem no corpo, não no header Authorization). Sempre 204,
    /// mesmo se o token já estava revogado ou não existia — <see cref="RevogarSessaoUseCase"/> é
    /// idempotente de propósito, pra não expor ao cliente se um dado token chegou a existir.
    /// </summary>
    [HttpPost("logout")]
    public async Task<IActionResult> Logout(
        [FromBody] LogoutRequest requisicao,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(requisicao.RefreshToken))
        {
            return Problem(
                title: "refreshToken é obrigatório.",
                statusCode: StatusCodes.Status400BadRequest);
        }

        await revogarSessao.ExecutarAsync(requisicao.RefreshToken, cancellationToken);

        return NoContent();
    }
}
