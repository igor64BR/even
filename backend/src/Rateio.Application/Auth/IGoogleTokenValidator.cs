namespace Rateio.Application.Auth;

/// <summary>
/// Abstrai a validação do ID token do Google (assinatura, emissor e audience). A implementação
/// concreta (Google.Apis.Auth) vive em Rateio.Infrastructure — Application não referencia a
/// biblioteca do Google, só esta interface (Dependency Inversion: facilita testar o resto do fluxo
/// sem token real e trocar de provedor sem tocar em casos de uso ou controller).
/// </summary>
public interface IGoogleTokenValidator
{
    /// <summary>
    /// Valida o ID token e extrai a identidade do usuário.
    /// </summary>
    /// <exception cref="GoogleTokenInvalidoException">
    /// Token com assinatura inválida, audience/emissor incorretos, expirado, ou e-mail não
    /// verificado.
    /// </exception>
    Task<GoogleUserInfo> ValidarAsync(string idToken, CancellationToken cancellationToken = default);
}
