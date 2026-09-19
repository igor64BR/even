using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rateio.Api.Contracts;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;

namespace Rateio.Api.Controllers;

/// <summary>
/// T18.1/T18.2: sincronização de um grupo local (Room, app) pro backend (RF09). T23.1 acrescentou
/// <c>POST /groups/{id}/expenses</c>: adicionar uma despesa avulsa a um grupo que já está
/// sincronizado (dia a dia, diferente do bulk de sincronização). O controller só traduz
/// HTTP↔caso de uso — toda orquestração vive nos casos de uso (<see cref="SincronizarGrupoUseCase"/>,
/// <see cref="CriarDespesaUseCase"/>).
/// </summary>
[ApiController]
[Authorize]
[Route("groups")]
public class GruposController(
    SincronizarGrupoUseCase sincronizarGrupo,
    CriarDespesaUseCase criarDespesa) : ControllerBase
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

    /// <summary>
    /// T23.1: adiciona uma despesa a um grupo já sincronizado (<paramref name="id"/> é o
    /// <c>GrupoId</c> do servidor, retornado por <c>POST /groups/sync</c>). RNF07 é checado no
    /// caso de uso (<see cref="CriarDespesaUseCase"/>) antes de qualquer persistência — 404 se o
    /// grupo não existe, 403 se o usuário autenticado não tem acesso a ele.
    /// </summary>
    [HttpPost("{id:guid}/expenses")]
    public async Task<ActionResult<CriarDespesaResponse>> CriarDespesa(
        Guid id,
        [FromBody] DespesaSincronizadaRequest requisicao,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var despesaId = await criarDespesa.ExecutarAsync(usuarioAutenticadoId, id, requisicao, cancellationToken);

            return Created($"/groups/{id}/expenses/{despesaId}", new CriarDespesaResponse(despesaId));
        }
        catch (GrupoNaoEncontradoException erro)
        {
            return Problem(
                title: "Grupo não encontrado.",
                detail: erro.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AcessoNegadoException erro)
        {
            return Problem(
                title: "Usuário sem acesso ao grupo.",
                detail: erro.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
        catch (Exception erro) when (erro is ArgumentException or InvalidOperationException)
        {
            // Mesmo padrão de Sync: payload malformado ou violando invariante de domínio nunca é
            // erro de servidor.
            return Problem(
                title: "Payload de despesa inválido.",
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
