using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rateio.Api.Contracts;
using Rateio.Application.Grupos;

namespace Rateio.Api.Controllers;

/// <summary>
/// T18.1/T18.2: sincronização de um grupo local (Room, app) pro backend (RF09). O controller só
/// traduz HTTP↔caso de uso — a orquestração (mapear payload pra domínio, marcar como sincronizado,
/// persistir) vive inteira em <see cref="SincronizarGrupoUseCase"/>.
/// </summary>
[ApiController]
[Authorize]
[Route("groups")]
public class GruposController(SincronizarGrupoUseCase sincronizarGrupo) : ControllerBase
{
    /// <summary>
    /// RNF07: o dono do grupo sincronizado é sempre o usuário do JWT validado
    /// (<see cref="ObterUsuarioAutenticadoId"/>), nunca um campo do corpo da requisição — não há
    /// como um usuário autenticado sincronizar um grupo em nome de outro.
    /// </summary>
    [HttpPost("sync")]
    public async Task<ActionResult<SincronizarGrupoResponse>> Sync(
        [FromBody] SincronizarGrupoRequest requisicao,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var grupoId = await sincronizarGrupo.ExecutarAsync(usuarioAutenticadoId, requisicao, cancellationToken);

            return Ok(new SincronizarGrupoResponse(grupoId));
        }
        catch (Exception erro) when (erro is ArgumentException or InvalidOperationException)
        {
            // Payload malformado ou violando uma invariante de domínio (ex.: nome vazio, grupo sem
            // participante, participação por peso sem peso) — nunca um erro de servidor.
            return Problem(
                title: "Payload de sincronização inválido.",
                detail: erro.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    private Guid ObterUsuarioAutenticadoId()
    {
        var valorClaimSub = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
            ?? throw new InvalidOperationException("JWT autenticado sem claim 'sub'.");

        return Guid.Parse(valorClaimSub);
    }
}
