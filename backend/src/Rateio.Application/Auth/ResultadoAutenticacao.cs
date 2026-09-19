namespace Rateio.Application.Auth;

/// <summary>
/// Resultado de um login bem-sucedido: os tokens que o app deve usar daí em diante e os dados do
/// usuário pra exibição imediata (evita o app precisar de uma segunda chamada só pra saber quem
/// logou).
/// </summary>
public sealed record ResultadoAutenticacao(string AccessToken, string RefreshToken, Usuario Usuario);
