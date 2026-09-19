namespace Rateio.Application.Auth;

/// <summary>
/// Abstrai a emissão do par de tokens da aplicação. A implementação concreta (assinatura JWT,
/// geração do refresh token opaco) vive em Rateio.Infrastructure — Application só conhece esta
/// interface, o que permite testar a emissão (expiração, distinção entre tokens) sem subir banco
/// nem validar um ID token real do Google.
/// </summary>
public interface IJwtIssuer
{
    /// <summary>
    /// Emite um novo access token (curto, RNF06: ≤ 15 min) e um novo refresh token (opaco) para o
    /// usuário informado. Não persiste nada — a persistência do refresh token (pra permitir
    /// revogação) é responsabilidade de <see cref="IRefreshTokenRepository"/>.
    /// </summary>
    ParDeTokens Emitir(Usuario usuario);
}
