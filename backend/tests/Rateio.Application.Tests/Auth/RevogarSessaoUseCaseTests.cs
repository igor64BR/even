using Moq;
using Rateio.Application.Auth;

namespace Rateio.Application.Tests.Auth;

/// <summary>
/// Cobre <see cref="RevogarSessaoUseCase"/> (T14.1) com <see cref="IRefreshTokenRepository"/>
/// mockado — mesmo estilo de <see cref="AutenticarComGoogleUseCaseTests"/>, sem banco real. A
/// idempotência em si (token já revogado ou desconhecido não é erro) é responsabilidade da
/// implementação de <see cref="IRefreshTokenRepository"/> (ver RefreshTokenRepository), não do
/// use case, que só repassa a chamada.
/// </summary>
public class RevogarSessaoUseCaseTests
{
    private readonly Mock<IRefreshTokenRepository> _refreshTokens = new();

    private RevogarSessaoUseCase CriarUseCase() => new(_refreshTokens.Object);

    [Fact]
    public async Task ExecutarAsync_ComRefreshTokenValido_ChamaRevogarAsyncComOMesmoToken()
    {
        var useCase = CriarUseCase();

        await useCase.ExecutarAsync("refresh-token-do-usuario");

        _refreshTokens.Verify(
            r => r.RevogarAsync("refresh-token-do-usuario", It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_PropagaCancellationTokenParaORepositorio()
    {
        using var cancellationTokenSource = new CancellationTokenSource();
        var useCase = CriarUseCase();

        await useCase.ExecutarAsync("refresh-token-do-usuario", cancellationTokenSource.Token);

        _refreshTokens.Verify(
            r => r.RevogarAsync("refresh-token-do-usuario", cancellationTokenSource.Token),
            Times.Once);
    }
}
