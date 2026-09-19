namespace Rateio.Infrastructure.Tests.Auth;

/// <summary>
/// <see cref="TimeProvider"/> de teste que sempre devolve o mesmo instante, pra dar pra afirmar
/// exatamente a expiração calculada por <c>JwtIssuer</c> sem depender do relógio real (evita testes
/// flaky por diferença de alguns milissegundos).
/// </summary>
internal sealed class RelogioFixo(DateTimeOffset agora) : TimeProvider
{
    public override DateTimeOffset GetUtcNow() => agora;
}
