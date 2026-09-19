namespace Rateio.Api.Contracts;

/// <summary>
/// Corpo de <c>POST /auth/logout</c>. O refresh token vai no corpo — não lido do header
/// <c>Authorization</c> — porque é ele, e não o access token, quem identifica a sessão a revogar
/// (é o que <see cref="Rateio.Application.Auth.IRefreshTokenRepository"/> guarda o hash de). Isso
/// também deixa o logout funcionar mesmo com o access token já expirado (RNF06: até 15 min),
/// exatamente o caso comum de "usuário abriu o app depois de um tempo e quer sair".
/// </summary>
public sealed record LogoutRequest(string RefreshToken);
