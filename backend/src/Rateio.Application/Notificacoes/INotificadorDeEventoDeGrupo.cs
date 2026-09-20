namespace Rateio.Application.Notificacoes;

/// <summary>
/// Abstração de notificação em tempo real (T38, constitution.md princípio 3) — Application
/// depende só disto, nunca de <c>Microsoft.AspNetCore.SignalR</c> diretamente (Dependency
/// Inversion: o detalhe de transporte é de fora pra dentro, não o contrário). A implementação real
/// com <c>IHubContext&lt;RateioHub&gt;</c> vive em <c>Rateio.Api</c> (onde o Hub também vive — ver
/// <c>Rateio.Api.Hubs.NotificadorDeEventoDeGrupoSignalR</c>); testes de caso de uso usam um
/// mock/fake desta interface, sem precisar de um Hub real rodando.
///
/// Falha ao notificar nunca deve impedir a operação de negócio que já foi persistida com sucesso —
/// é responsabilidade da implementação (não deste contrato) decidir como lidar com uma falha de
/// entrega (ex.: logar e engolir), porque RF35/RF36 é "avisar em tempo real", não "garantir
/// entrega": a limitação de kill-state (constitution.md princípio 3) já documenta que a entrega via
/// Hub não é garantida, e o fallback de pull (T39) cobre o que se perde.
/// </summary>
public interface INotificadorDeEventoDeGrupo
{
    Task NotificarAsync(IEventoDeGrupo evento, CancellationToken cancellationToken = default);
}
