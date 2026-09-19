namespace Rateio.Application.Auth;

/// <summary>
/// Persistência das sessões (refresh tokens) emitidas, pra permitir revogação futura (T14). A
/// implementação concreta nunca guarda o refresh token em texto puro — só um hash dele.
/// </summary>
public interface IRefreshTokenRepository
{
    Task SalvarAsync(
        Guid usuarioId,
        string refreshToken,
        DateTimeOffset expiraEm,
        CancellationToken cancellationToken = default);
}
