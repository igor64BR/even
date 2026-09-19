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
/// sincronizado (dia a dia, diferente do bulk de sincronização). T21 acrescentou o convite/link de
/// grupo (RF07): <c>POST /groups/{id}/invite-code</c> (só o dono) e
/// <c>POST /groups/join/{codigo}</c> (qualquer autenticado). O controller só traduz HTTP↔caso de
/// uso — toda orquestração vive nos casos de uso (<see cref="SincronizarGrupoUseCase"/>,
/// <see cref="CriarDespesaUseCase"/>, <see cref="GerarCodigoConviteUseCase"/>,
/// <see cref="EntrarNoGrupoViaConviteUseCase"/>).
/// </summary>
[ApiController]
[Authorize]
[Route("groups")]
public class GruposController(
    SincronizarGrupoUseCase sincronizarGrupo,
    CriarDespesaUseCase criarDespesa,
    GerarCodigoConviteUseCase gerarCodigoConvite,
    EntrarNoGrupoViaConviteUseCase entrarNoGrupoViaConvite) : ControllerBase
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

    /// <summary>
    /// T21.1: gera (ou substitui — ver <c>CodigoConvite</c>, "um código ativo por grupo") o código
    /// de convite do grupo <paramref name="id"/>. RNF07/T21: só o dono gera código —
    /// <see cref="GerarCodigoConviteUseCase"/> lança <see cref="AcessoNegadoException"/> (403) pra
    /// qualquer outro usuário autenticado, mesmo que ele já seja participante.
    /// </summary>
    [HttpPost("{id:guid}/invite-code")]
    public async Task<ActionResult<GerarCodigoConviteResponse>> GerarCodigoConvite(
        Guid id,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var codigo = await gerarCodigoConvite.ExecutarAsync(usuarioAutenticadoId, id, cancellationToken);

            return Ok(new GerarCodigoConviteResponse(codigo.Valor, codigo.ExpiraEm));
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
                title: "Só o dono do grupo pode gerar código de convite.",
                detail: erro.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// T21.2: resolve <paramref name="codigo"/> e adiciona o usuário autenticado como novo
    /// participante do grupo correspondente (RF07). Aberto a qualquer usuário autenticado — não há
    /// checagem de dono/acesso aqui, o código válido É a autorização (RNF07/T21: "entrar via
    /// código é aberto a qualquer autenticado, esse é o ponto do recurso"). Resposta no mesmo
    /// formato de <c>POST /groups/sync</c> (T18), pro app já poder mostrar o grupo.
    /// </summary>
    [HttpPost("join/{codigo}")]
    public async Task<ActionResult<SincronizarGrupoResponse>> EntrarComCodigo(
        string codigo,
        CancellationToken cancellationToken)
    {
        try
        {
            var nomeDoUsuarioAutenticado = ObterNomeDoUsuarioAutenticado();

            var grupoId = await entrarNoGrupoViaConvite.ExecutarAsync(codigo, nomeDoUsuarioAutenticado, cancellationToken);

            return Ok(new SincronizarGrupoResponse(grupoId));
        }
        catch (CodigoConviteInvalidoException erro)
        {
            return Problem(
                title: "Código de convite inválido.",
                detail: erro.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (GrupoNaoEncontradoException erro)
        {
            // Defensivo: só ocorreria se o grupo do convite tivesse sido excluído entre a
            // resolução do código e a leitura do grupo — o schema já protege isso via cascade
            // (CodigoConviteEntityConfiguration), mas o caso de uso não assume isso silenciosamente.
            return Problem(
                title: "Grupo não encontrado.",
                detail: erro.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (Exception erro) when (erro is ArgumentException or InvalidOperationException)
        {
            return Problem(
                title: "Não foi possível entrar no grupo.",
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

    /// <summary>
    /// T21.2: nome do novo <c>Participante.Autenticado</c> (T15) vem do claim <c>name</c> do JWT —
    /// o mesmo valor que <c>JwtIssuer</c> (T11) grava a partir de <c>Usuario.Nome</c> na emissão do
    /// token, sem precisar de uma nova consulta a <c>IUsuarioRepository</c> só pra reobter o nome.
    /// </summary>
    private string ObterNomeDoUsuarioAutenticado() =>
        User.FindFirstValue(JwtRegisteredClaimNames.Name)
            ?? throw new InvalidOperationException("JWT autenticado sem claim 'name'.");
}
