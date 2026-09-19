namespace Rateio.Application.Grupos;

/// <summary>
/// Persistência de grupos sincronizados. Implementação concreta (EF Core) em
/// Rateio.Infrastructure — Application só conhece esta abstração (SOLID: dependency inversion),
/// igual ao padrão já estabelecido por <c>Rateio.Application.Auth.IUsuarioRepository</c>.
/// </summary>
public interface IGrupoRepository
{
    /// <summary>
    /// Persiste o grupo (com participantes e despesas) pela primeira vez e retorna o id do grupo
    /// no servidor — que nunca é o id local do Room, e sim o novo <see cref="Rateio.Domain.Grupo.Id"/>
    /// gerado por <see cref="Rateio.Domain.Grupo.Criar"/> (T18.1: "response: id do grupo no
    /// servidor, pro app saber qual GrupoId remoto corresponde ao grupo local").
    /// </summary>
    Task<Guid> SincronizarAsync(GrupoParaSincronizar grupo, CancellationToken cancellationToken = default);
}
