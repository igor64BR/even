namespace Rateio.Application.Auth;

/// <summary>
/// Orquestra o logout (T14): revoga a sessão associada a um refresh token. Existe como caso de uso
/// próprio — em vez do controller chamar <see cref="IRefreshTokenRepository"/> direto — pra manter
/// o mesmo padrão de <see cref="AutenticarComGoogleUseCase"/> (controller só traduz HTTP, a regra
/// de negócio mora aqui) e dar um ponto único de extensão se o logout um dia precisar fazer mais
/// que revogar (ex.: notificar outras sessões via SignalR).
/// </summary>
public sealed class RevogarSessaoUseCase(IRefreshTokenRepository refreshTokens)
{
    public Task ExecutarAsync(string refreshToken, CancellationToken cancellationToken = default) =>
        refreshTokens.RevogarAsync(refreshToken, cancellationToken);
}
