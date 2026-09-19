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

    /// <summary>
    /// Marca <c>RevogadoEm</c> na sessão correspondente a <paramref name="refreshToken"/> (T14).
    /// Idempotente: token já revogado ou desconhecido não é erro, só não faz nada — evita expor ao
    /// chamador se um dado token chegou a existir.
    /// </summary>
    Task RevogarAsync(
        string refreshToken,
        CancellationToken cancellationToken = default);
}
