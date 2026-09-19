using Rateio.Application.Auth;

namespace Rateio.Api.Contracts;

/// <summary>Resposta de sucesso de <c>POST /auth/google</c>.</summary>
public sealed record GoogleLoginResponse(string AccessToken, string RefreshToken, UsuarioResponse User)
{
    public static GoogleLoginResponse De(ResultadoAutenticacao resultado) => new(
        resultado.AccessToken,
        resultado.RefreshToken,
        new UsuarioResponse(resultado.Usuario.Nome, resultado.Usuario.Email));
}

public sealed record UsuarioResponse(string Nome, string Email);
