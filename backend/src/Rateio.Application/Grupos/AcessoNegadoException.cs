namespace Rateio.Application.Grupos;

/// <summary>
/// RNF07: o usuário autenticado não tem acesso ao grupo (ver <see cref="AcessoAoGrupo"/> pra qual
/// vínculo é checado hoje). Nunca vira 500 — o controller mapeia isso pra 403.
/// </summary>
public sealed class AcessoNegadoException(Guid usuarioId, Guid grupoId)
    : Exception($"Usuário {usuarioId} não tem acesso ao grupo {grupoId}.")
{
    public Guid UsuarioId { get; } = usuarioId;

    public Guid GrupoId { get; } = grupoId;
}
