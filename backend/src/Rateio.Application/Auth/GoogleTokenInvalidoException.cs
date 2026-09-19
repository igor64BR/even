namespace Rateio.Application.Auth;

/// <summary>
/// ID token do Google rejeitado — assinatura inválida, audience errada, emissor errado, expirado,
/// ou e-mail não verificado pelo Google. A mensagem nunca deve incluir o token em si.
/// </summary>
public sealed class GoogleTokenInvalidoException(string mensagem) : Exception(mensagem);
