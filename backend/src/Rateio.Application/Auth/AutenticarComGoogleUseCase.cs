namespace Rateio.Application.Auth;

/// <summary>
/// Orquestra o login via Google (T11): valida o ID token, garante que existe um usuário local
/// correspondente, emite o par de tokens da aplicação e persiste o refresh token pra permitir
/// revogação depois. É aqui — não no controller — que a sequência de passos e as regras de
/// "usuário novo vs. existente" vivem, mantendo o controller só como orquestrador HTTP.
/// </summary>
public sealed class AutenticarComGoogleUseCase(
    IGoogleTokenValidator validadorGoogle,
    IUsuarioRepository usuarios,
    IJwtIssuer jwtIssuer,
    IRefreshTokenRepository refreshTokens)
{
    public async Task<ResultadoAutenticacao> ExecutarAsync(string idToken, CancellationToken cancellationToken = default)
    {
        var dadosGoogle = await validadorGoogle.ValidarAsync(idToken, cancellationToken);
        var usuario = await ObterOuCriarUsuarioAsync(dadosGoogle, cancellationToken);
        var tokens = jwtIssuer.Emitir(usuario);

        await refreshTokens.SalvarAsync(usuario.Id, tokens.RefreshToken, tokens.RefreshTokenExpiraEm, cancellationToken);

        return new ResultadoAutenticacao(tokens.AccessToken, tokens.RefreshToken, usuario);
    }

    private async Task<Usuario> ObterOuCriarUsuarioAsync(GoogleUserInfo dadosGoogle, CancellationToken cancellationToken)
    {
        var usuarioExistente = await usuarios.ObterPorGoogleSubjectIdAsync(dadosGoogle.GoogleSubjectId, cancellationToken);

        return usuarioExistente ?? await usuarios.CriarAsync(dadosGoogle, cancellationToken);
    }
}
