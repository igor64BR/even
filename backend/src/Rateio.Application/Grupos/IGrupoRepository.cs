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

    /// <summary>
    /// Resumo de acesso do grupo (T23), usado pra validar RNF07 antes de operações do dia a dia
    /// como adicionar despesa — sem carregar participantes/despesas, que quem chama aqui não
    /// precisa. <c>null</c> quando o grupo não existe; quem chama decide se isso vira 404.
    /// </summary>
    Task<AcessoAoGrupo?> ObterAcessoAsync(Guid grupoId, CancellationToken cancellationToken = default);
}
