namespace Rateio.Application.Notificacoes;

/// <summary>
/// Contrato comum dos eventos de grupo em tempo real (T38, RF35/RF36) — cada evento concreto
/// (<see cref="EventoDespesaCriada"/>, <see cref="EventoDividaQuitada"/>) é um tipo próprio, nunca
/// um <c>object</c>/<c>dynamic</c> solto sendo passado adiante. <see cref="GrupoId"/> é o que
/// <see cref="INotificadorDeEventoDeGrupo"/> usa pra escolher o grupo SignalR de destino sem
/// precisar de um segundo parâmetro redundante em toda chamada.
/// </summary>
public interface IEventoDeGrupo
{
    TipoEventoDeGrupo Tipo { get; }

    Guid GrupoId { get; }
}
