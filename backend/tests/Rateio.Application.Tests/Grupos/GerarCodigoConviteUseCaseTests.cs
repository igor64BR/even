using Moq;
using Rateio.Application.Grupos;

namespace Rateio.Application.Tests.Grupos;

/// <summary>
/// Cobre T21.1 com <see cref="IGrupoRepository"/>/<see cref="ICodigoConviteRepository"/> mockados
/// (mesmo padrão de <c>CriarDespesaUseCaseTests</c>, T23): nenhum teste aqui toca EF/Postgres. O
/// entregável da task — "gerar código funciona só pro dono (outro usuário recebe 403)" — é o foco
/// dos dois primeiros testes; os demais cobrem grupo inexistente (404) e a política de expiração
/// documentada em <see cref="CodigoConvite"/>.
/// </summary>
public class GerarCodigoConviteUseCaseTests
{
    private static readonly DateTimeOffset Agora = new(2026, 1, 10, 12, 0, 0, TimeSpan.Zero);

    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<ICodigoConviteRepository> _codigoConviteRepository = new();
    private readonly RelogioFixo _relogio = new(Agora);

    private GerarCodigoConviteUseCase CriarUseCase() =>
        new(_grupoRepository.Object, _codigoConviteRepository.Object, _relogio);

    [Fact]
    public async Task ExecutarAsync_DonoDoGrupo_GeraEPersisteCodigoValidoPorSeteDias()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        CodigoConvite? salvo = null;
        _codigoConviteRepository
            .Setup(r => r.SalvarAsync(It.IsAny<CodigoConvite>(), It.IsAny<CancellationToken>()))
            .Callback<CodigoConvite, CancellationToken>((codigo, _) => salvo = codigo)
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();

        var resultado = await useCase.ExecutarAsync(donoUsuarioId, grupoId);

        Assert.NotNull(salvo);
        Assert.Equal(resultado, salvo);
        Assert.Equal(grupoId, salvo!.GrupoId);
        Assert.Equal(8, salvo.Valor.Length);
        Assert.Equal(Agora, salvo.CriadoEm);
        Assert.Equal(Agora.AddDays(7), salvo.ExpiraEm);
        _codigoConviteRepository.Verify(
            r => r.SalvarAsync(It.IsAny<CodigoConvite>(), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioQueNaoEODono_LancaAcessoNegadoSemGerarCodigo()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var outroUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(outroUsuarioId, grupoId));

        Assert.Equal(outroUsuarioId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _codigoConviteRepository.Verify(
            r => r.SalvarAsync(It.IsAny<CodigoConvite>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemGerarCodigo()
    {
        var grupoId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId));

        Assert.Equal(grupoId, excecao.GrupoId);
        _codigoConviteRepository.Verify(
            r => r.SalvarAsync(It.IsAny<CodigoConvite>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
