namespace Rateio.Application.Notificacoes;

/// <summary>
/// Discrimina o tipo de <see cref="IEventoDeGrupo"/> sem recorrer a <c>object</c>/<c>dynamic</c> ou
/// a type-checking solto (<c>is</c>/pattern matching) em todo consumidor — RF35/RF36 (T38). O nome
/// de cada valor é o mesmo usado como nome do método SignalR que o cliente escuta
/// (<c>connection.on("DespesaCriada", ...)</c>), mas o campo em si serve pra qualquer consumidor que
/// não tenha acesso ao "nome do método" (ex.: T39, fallback de pull, se vier a persistir o evento).
/// </summary>
public enum TipoEventoDeGrupo
{
    DespesaCriada,
    DividaQuitada,
}
