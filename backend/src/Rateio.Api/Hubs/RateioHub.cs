using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Rateio.Application.Grupos;

namespace Rateio.Api.Hubs;

/// <summary>
/// T38.1: Hub de notificação em tempo real (RF35/RF36; constitution.md princípio 3 — sistema
/// próprio via SignalR, sem push de terceiros). A conexão exige o mesmo JWT Bearer já validado pelo
/// resto da API: <see cref="AuthorizeAttribute"/> reaproveita o esquema padrão configurado em
/// <c>Rateio.Infrastructure.DependencyInjection.AddAutenticacaoJwt</c> (T18) — a única diferença é
/// que um cliente de Hub não manda header <c>Authorization</c> no handshake de WebSocket, então o
/// token chega via query string <c>access_token</c> (lido pelo <c>OnMessageReceived</c> adicionado
/// naquele mesmo método, restrito a <see cref="Infrastructure.Notificacoes.RotaDoHubDeNotificacoes.Caminho"/>).
///
/// Um grupo SignalR por <c>grupoId</c> (<see cref="Hub.Groups"/>) — nenhuma conexão é inscrita
/// automaticamente; o cliente entra explicitamente em cada grupo sincronizado que está acompanhando
/// via <see cref="EntrarNoGrupoAsync"/>. Essa entrada revalida RNF07
/// (<see cref="AcessoAoGrupo.PertenceA"/>, a mesma regra que <c>CriarDespesaUseCase</c>/
/// <c>RegistrarQuitacaoUseCase</c> já checam antes de qualquer operação) — sem isso, qualquer
/// usuário autenticado poderia se inscrever no canal de tempo real de um grupo alheio e ver
/// descrição/valor de despesas e quitações de terceiros só por adivinhar o <c>grupoId</c>.
/// </summary>
[Authorize]
public sealed class RateioHub(IGrupoRepository grupoRepository) : Hub
{
    public static string NomeDoGrupo(Guid grupoId) => $"grupo:{grupoId}";

    public async Task EntrarNoGrupoAsync(Guid grupoId)
    {
        var usuarioAutenticadoId = ObterUsuarioAutenticadoId();
        var acesso = await grupoRepository.ObterAcessoAsync(grupoId, Context.ConnectionAborted);

        if (acesso is null || !acesso.PertenceA(usuarioAutenticadoId))
        {
            // HubException é o único tipo de exceção cujo Message chega ao cliente SignalR — não
            // vazamos detalhe de "existe mas não é seu" vs. "não existe" (o mesmo cuidado que os
            // use cases fazem via exceções distintas não se aplica aqui: no Hub, distinguir os dois
            // casos só ajudaria alguém tentando enumerar grupoIds alheios).
            throw new HubException("Grupo inexistente ou sem acesso.");
        }

        await Groups.AddToGroupAsync(Context.ConnectionId, NomeDoGrupo(grupoId));
    }

    public Task SairDoGrupoAsync(Guid grupoId) =>
        Groups.RemoveFromGroupAsync(Context.ConnectionId, NomeDoGrupo(grupoId));

    private Guid ObterUsuarioAutenticadoId()
    {
        // Mesmo padrão de Rateio.Api.Controllers.GruposController.ObterUsuarioAutenticadoId —
        // claim "sub" lida ao pé da letra porque AddAutenticacaoJwt desliga MapInboundClaims.
        var valorClaimSub = Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub)
            ?? throw new HubException("JWT autenticado sem claim 'sub'.");

        return Guid.Parse(valorClaimSub);
    }
}
