using Microsoft.AspNetCore.SignalR;
using Rateio.Application.Notificacoes;

namespace Rateio.Api.Hubs;

/// <summary>
/// T38.2/T38.3: implementação real de <see cref="INotificadorDeEventoDeGrupo"/> usando
/// <see cref="IHubContext{RateioHub}"/> — vive em <c>Rateio.Api</c> (não em Rateio.Infrastructure)
/// porque é aqui que <see cref="RateioHub"/> também vive; nada em Rateio.Application referencia
/// <c>Microsoft.AspNetCore.SignalR</c> (Dependency Inversion: os use cases dependem só da
/// abstração, esta classe é o único ponto do projeto que conhece SignalR concretamente pra emitir
/// eventos).
///
/// O nome do método SignalR enviado ao grupo é <c>evento.Tipo</c> (<see cref="TipoEventoDeGrupo"/>)
/// — <c>nameof(TipoEventoDeGrupo.DespesaCriada)</c>/<c>nameof(TipoEventoDeGrupo.DividaQuitada)</c>,
/// não o nome da classe do evento (<c>nameof(EventoDespesaCriada)</c> seria
/// <c>"EventoDespesaCriada"</c>, um nome diferente) — o cliente assina exatamente esses nomes via
/// <c>connection.on("DespesaCriada", ...)</c>/<c>connection.on("DividaQuitada", ...)</c> (T40); um
/// método com nome errado chega ao cliente como invocação não-reconhecida (SignalR loga e descarta
/// em vez de lançar), então esse desalinhamento não aparece como falha do lado do servidor — só um
/// teste manual de ponta a ponta (cliente real recebendo o evento) pega isso.
/// </summary>
public sealed class NotificadorDeEventoDeGrupoSignalR(
    IHubContext<RateioHub> hubContext,
    ILogger<NotificadorDeEventoDeGrupoSignalR> logger) : INotificadorDeEventoDeGrupo
{
    public async Task NotificarAsync(IEventoDeGrupo evento, CancellationToken cancellationToken = default)
    {
        try
        {
            var grupo = hubContext.Clients.Group(RateioHub.NomeDoGrupo(evento.GrupoId));

            await (evento switch
            {
                EventoDespesaCriada despesaCriada => grupo.SendAsync(
                    nameof(TipoEventoDeGrupo.DespesaCriada), despesaCriada, cancellationToken),
                EventoDividaQuitada dividaQuitada => grupo.SendAsync(
                    nameof(TipoEventoDeGrupo.DividaQuitada), dividaQuitada, cancellationToken),
                _ => throw new ArgumentOutOfRangeException(
                    nameof(evento), evento.Tipo, "Tipo de evento de grupo sem envio SignalR mapeado."),
            });
        }
        catch (Exception erro) when (erro is not ArgumentOutOfRangeException)
        {
            // A operação de negócio (despesa/quitação) já foi persistida com sucesso antes desta
            // chamada — uma falha de entrega em tempo real (ex.: hub indisponível) nunca deve
            // derrubar o use case. RF35/RF36 é "avisar em tempo real", não "garantir entrega"; a
            // limitação de kill-state/desconexão já é um trade-off documentado (constitution.md
            // princípio 3) e o fallback de pull (T39) cobre o que se perde aqui.
            logger.LogWarning(
                erro,
                "Falha ao notificar evento {TipoEvento} do grupo {GrupoId} via SignalR.",
                evento.Tipo,
                evento.GrupoId);
        }
    }
}
