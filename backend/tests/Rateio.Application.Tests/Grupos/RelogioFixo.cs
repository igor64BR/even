namespace Rateio.Application.Tests.Grupos;

/// <summary>
/// <see cref="TimeProvider"/> de teste que sempre devolve o mesmo instante — mesmo padrão de
/// <c>Rateio.Infrastructure.Tests.Auth.RelogioFixo</c> (T11), reproduzido aqui porque os dois
/// projetos de teste não compartilham código entre si.
/// </summary>
internal sealed class RelogioFixo(DateTimeOffset agora) : TimeProvider
{
    public override DateTimeOffset GetUtcNow() => agora;
}
