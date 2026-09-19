using Moq;
using Rateio.Application.Auth;

namespace Rateio.Application.Tests.Auth;

/// <summary>
/// Cobre a orquestração de <see cref="AutenticarComGoogleUseCase"/> (T11) inteiramente com dublês —
/// nenhum teste aqui precisa de um ID token real do Google nem de banco. O caminho de "token
/// inválido/expirado" é simulado fazendo o <see cref="IGoogleTokenValidator"/> mockado lançar
/// <see cref="GoogleTokenInvalidoException"/>, exatamente como a implementação real faria.
/// </summary>
public class AutenticarComGoogleUseCaseTests
{
    private static readonly GoogleUserInfo DadosGoogle = new("google-sub-123", "Igor Baiocco", "igor@example.com");

    private readonly Mock<IGoogleTokenValidator> _validadorGoogle = new();
    private readonly Mock<IUsuarioRepository> _usuarios = new();
    private readonly Mock<IJwtIssuer> _jwtIssuer = new();
    private readonly Mock<IRefreshTokenRepository> _refreshTokens = new();

    private AutenticarComGoogleUseCase CriarUseCase() => new(
        _validadorGoogle.Object,
        _usuarios.Object,
        _jwtIssuer.Object,
        _refreshTokens.Object);

    [Fact]
    public async Task ExecutarAsync_ComTokenInvalidoOuExpirado_PropagaGoogleTokenInvalidoExceptionSemChamarMaisNada()
    {
        _validadorGoogle
            .Setup(v => v.ValidarAsync("id-token-invalido", It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GoogleTokenInvalidoException("ID token do Google rejeitado: token expirado."));

        var useCase = CriarUseCase();

        await Assert.ThrowsAsync<GoogleTokenInvalidoException>(
            () => useCase.ExecutarAsync("id-token-invalido"));

        _usuarios.Verify(u => u.ObterPorGoogleSubjectIdAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
        _jwtIssuer.Verify(j => j.Emitir(It.IsAny<Usuario>()), Times.Never);
        _refreshTokens.Verify(
            r => r.SalvarAsync(It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioNovo_CriaUsuarioEmitEPersisteRefreshToken()
    {
        var usuarioCriado = new Usuario(Guid.NewGuid(), DadosGoogle.GoogleSubjectId, DadosGoogle.Nome, DadosGoogle.Email);
        var tokens = new ParDeTokens(
            "access-token",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "refresh-token",
            DateTimeOffset.UtcNow.AddDays(30));

        _validadorGoogle.Setup(v => v.ValidarAsync("id-token-valido", It.IsAny<CancellationToken>()))
            .ReturnsAsync(DadosGoogle);
        _usuarios.Setup(u => u.ObterPorGoogleSubjectIdAsync(DadosGoogle.GoogleSubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Usuario?)null);
        _usuarios.Setup(u => u.CriarAsync(DadosGoogle, It.IsAny<CancellationToken>()))
            .ReturnsAsync(usuarioCriado);
        _jwtIssuer.Setup(j => j.Emitir(usuarioCriado)).Returns(tokens);

        var useCase = CriarUseCase();

        var resultado = await useCase.ExecutarAsync("id-token-valido");

        Assert.Equal(tokens.AccessToken, resultado.AccessToken);
        Assert.Equal(tokens.RefreshToken, resultado.RefreshToken);
        Assert.Equal(usuarioCriado, resultado.Usuario);
        _usuarios.Verify(u => u.CriarAsync(DadosGoogle, It.IsAny<CancellationToken>()), Times.Once);
        _refreshTokens.Verify(
            r => r.SalvarAsync(usuarioCriado.Id, tokens.RefreshToken, tokens.RefreshTokenExpiraEm, It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioJaExistente_NaoCriaUsuarioDeNovo()
    {
        var usuarioExistente = new Usuario(Guid.NewGuid(), DadosGoogle.GoogleSubjectId, DadosGoogle.Nome, DadosGoogle.Email);
        var tokens = new ParDeTokens(
            "access-token",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "refresh-token",
            DateTimeOffset.UtcNow.AddDays(30));

        _validadorGoogle.Setup(v => v.ValidarAsync("id-token-valido", It.IsAny<CancellationToken>()))
            .ReturnsAsync(DadosGoogle);
        _usuarios.Setup(u => u.ObterPorGoogleSubjectIdAsync(DadosGoogle.GoogleSubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(usuarioExistente);
        _jwtIssuer.Setup(j => j.Emitir(usuarioExistente)).Returns(tokens);

        var useCase = CriarUseCase();

        await useCase.ExecutarAsync("id-token-valido");

        _usuarios.Verify(u => u.CriarAsync(It.IsAny<GoogleUserInfo>(), It.IsAny<CancellationToken>()), Times.Never);
    }
}
