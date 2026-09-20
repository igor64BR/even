using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rateio.Api.Contracts;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;
using Rateio.Application.Quitacoes;
using Rateio.Application.Simplificacao;

namespace Rateio.Api.Controllers;

/// <summary>
/// T18.1/T18.2: sincronização de um grupo local (Room, app) pro backend (RF09). T23.1 acrescentou
/// <c>POST /groups/{id}/expenses</c>: adicionar uma despesa avulsa a um grupo que já está
/// sincronizado (dia a dia, diferente do bulk de sincronização). T21 acrescentou o convite/link de
/// grupo (RF07): <c>POST /groups/{id}/invite-code</c> (só o dono) e
/// <c>POST /groups/join/{codigo}</c> (qualquer autenticado). T32.1 acrescentou
/// <c>GET /groups/{id}/settlement</c>: a simplificação de dívidas do grupo, recomputada sob
/// demanda (nunca armazenada). T35.1 acrescentou <c>POST /groups/{id}/settlements</c>: registra que
/// uma das transações sugeridas foi paga. O controller só traduz HTTP↔caso de uso — toda
/// orquestração vive nos casos de uso (<see cref="SincronizarGrupoUseCase"/>,
/// <see cref="CriarDespesaUseCase"/>, <see cref="GerarCodigoConviteUseCase"/>,
/// <see cref="EntrarNoGrupoViaConviteUseCase"/>, <see cref="ObterSimplificacaoDeDividasUseCase"/>,
/// <see cref="RegistrarQuitacaoUseCase"/>). T39.1 acrescentou
/// <c>GET /groups/{id}/events?desde={timestampIso8601}</c>: fallback de pull dos eventos de
/// despesa/quitação perdidos enquanto o app estava desconectado do Hub SignalR (T38,
/// constitution.md princípio 3) — ver <see cref="ObterEventosDeGrupoUseCase"/>.
/// </summary>
[ApiController]
[Authorize]
[Route("groups")]
public class GruposController(
    SincronizarGrupoUseCase sincronizarGrupo,
    CriarDespesaUseCase criarDespesa,
    GerarCodigoConviteUseCase gerarCodigoConvite,
    EntrarNoGrupoViaConviteUseCase entrarNoGrupoViaConvite,
    ObterSimplificacaoDeDividasUseCase obterSimplificacaoDeDividas,
    RegistrarQuitacaoUseCase registrarQuitacao,
    ObterEventosDeGrupoUseCase obterEventosDeGrupo) : ControllerBase
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
    /// T32.1: simplificação de dívidas do grupo, sempre recomputada a partir do histórico vigente
    /// de despesas e quitações (motor de T31 — este endpoint não guarda saldo nenhum). Mesmo
    /// padrão de acesso/erro de <see cref="CriarDespesa"/>: 404 se o grupo não existe, 403 se o
    /// usuário autenticado não é dono dele (RNF07).
    /// </summary>
    [HttpGet("{id:guid}/settlement")]
    public async Task<ActionResult<IReadOnlyList<TransacaoResponse>>> ObterSettlement(
        Guid id,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var transacoes = await obterSimplificacaoDeDividas.ExecutarAsync(usuarioAutenticadoId, id, cancellationToken);

            var resposta = transacoes
                .Select(transacao => new TransacaoResponse(transacao.De.Valor, transacao.Para.Valor, transacao.Valor.Centavos))
                .ToList();

            return Ok(resposta);
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
    }

    /// <summary>
    /// T35.1: registra que uma das transações sugeridas por <see cref="ObterSettlement"/> foi paga
    /// (RF31/RF33) — <paramref name="requisicao"/> é <c>{ deParticipanteId, paraParticipanteId,
    /// valorCentavos }</c>. Mesmo padrão de acesso/erro de <see cref="CriarDespesa"/>: 404 se o
    /// grupo não existe, 403 se o usuário autenticado não tem acesso a ele (RNF07), 400 se o
    /// payload viola uma invariante de domínio (valor não positivo, pagador igual ao recebedor).
    /// Não devolve saldo recalculado — a próxima chamada a <c>GET /groups/{id}/settlement</c> já
    /// reflete a quitação (T35.2).
    /// </summary>
    [HttpPost("{id:guid}/settlements")]
    public async Task<ActionResult<RegistrarQuitacaoResponse>> RegistrarQuitacao(
        Guid id,
        [FromBody] RegistrarQuitacaoRequest requisicao,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var quitacaoId = await registrarQuitacao.ExecutarAsync(usuarioAutenticadoId, id, requisicao, cancellationToken);

            return Created($"/groups/{id}/settlements/{quitacaoId}", new RegistrarQuitacaoResponse(quitacaoId));
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
        catch (ArgumentException erro)
        {
            // Mesmo padrão de CriarDespesa: payload violando invariante de domínio nunca é erro de
            // servidor.
            return Problem(
                title: "Payload de quitação inválido.",
                detail: erro.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    /// <summary>
    /// T39.1: <c>GET /groups/{id}/events?desde={timestampIso8601}</c> — fallback de pull
    /// (constitution.md princípio 3): eventos de despesa/quitação do grupo persistidos no servidor
    /// depois de <paramref name="desde"/>, ordenados por data crescente, no mesmo formato que T38 já
    /// envia em tempo real via SignalR (<see cref="EventoDespesaCriada"/>/<see cref="EventoDividaQuitada"/>)
    /// — o app usa isso pra recuperar o que perdeu enquanto desconectado do Hub (T40). Mesmo padrão
    /// de acesso/erro de <see cref="ObterSettlement"/>: 404 se o grupo não existe, 403 se o usuário
    /// autenticado não tem acesso a ele (RNF07). Grupo sem eventos novos devolve lista vazia (200),
    /// nunca 404.
    ///
    /// A resposta é projetada para <c>IReadOnlyList&lt;object&gt;</c> (não
    /// <c>IReadOnlyList&lt;IEventoDeGrupo&gt;</c>) porque <c>System.Text.Json</c> serializa cada
    /// elemento de uma coleção pelo tipo declarado, não pelo tipo concreto em tempo de execução —
    /// devolver a interface faria o serializador incluir só <c>Tipo</c>/<c>GrupoId</c> (os membros
    /// da interface) e descartar <c>Descricao</c>/<c>ValorTotalCentavos</c>/etc. Declarar cada item
    /// como <c>object</c> força o serializador a usar o tipo concreto de cada evento — mesmo cuidado
    /// que <c>NotificadorDeEventoDeGrupoSignalR</c> (T38) já toma, só que lá via <c>switch</c>
    /// despachando pro tipo concreto em cada chamada a <c>SendAsync</c>.
    /// </summary>
    [HttpGet("{id:guid}/events")]
    public async Task<ActionResult<IReadOnlyList<object>>> ObterEventos(
        Guid id,
        [FromQuery] DateTimeOffset desde,
        CancellationToken cancellationToken)
    {
        try
        {
            var usuarioAutenticadoId = ObterUsuarioAutenticadoId();

            var eventos = await obterEventosDeGrupo.ExecutarAsync(usuarioAutenticadoId, id, desde, cancellationToken);

            return Ok(eventos.Cast<object>().ToList());
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
