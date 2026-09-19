namespace Rateio.Application.Auth;

/// <summary>
/// Dados extraídos do ID token do Google depois que a assinatura e a audience já foram validadas.
/// Não carrega nenhum segredo (nunca o token em si) — só o que o backend precisa pra
/// identificar/criar o usuário local.
/// </summary>
public sealed record GoogleUserInfo(string GoogleSubjectId, string Nome, string Email);
