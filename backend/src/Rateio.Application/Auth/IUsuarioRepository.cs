namespace Rateio.Application.Auth;

/// <summary>
/// Persistência de usuários autenticados via Google. Implementação concreta (EF Core) em
/// Rateio.Infrastructure.
/// </summary>
public interface IUsuarioRepository
{
    Task<Usuario?> ObterPorGoogleSubjectIdAsync(string googleSubjectId, CancellationToken cancellationToken = default);

    Task<Usuario> CriarAsync(GoogleUserInfo dadosGoogle, CancellationToken cancellationToken = default);
}
