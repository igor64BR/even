namespace Rateio.Application.Auth;

/// <summary>
/// Par de tokens emitido pela aplicação após autenticar um <see cref="Usuario"/>. O access token é
/// um JWT de vida curta (RNF06: expira em ≤ 15 min); o refresh token é opaco e guardado (com hash)
/// pra poder ser revogado depois (T14).
/// </summary>
public sealed record ParDeTokens(
    string AccessToken,
    DateTimeOffset AccessTokenExpiraEm,
    string RefreshToken,
    DateTimeOffset RefreshTokenExpiraEm);
