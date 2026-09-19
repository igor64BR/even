namespace Rateio.Application.Grupos;

/// <summary>
/// O grupo referenciado (ex.: <c>POST /groups/{id}/expenses</c>, T23) não existe no backend. Nunca
/// vira 500 — o controller mapeia isso pra 404 (mesmo padrão de <c>GoogleTokenInvalidoException</c>
/// virando 401 em <c>AuthController</c>).
/// </summary>
public sealed class GrupoNaoEncontradoException(Guid grupoId) : Exception($"Grupo {grupoId} não encontrado.")
{
    public Guid GrupoId { get; } = grupoId;
}
