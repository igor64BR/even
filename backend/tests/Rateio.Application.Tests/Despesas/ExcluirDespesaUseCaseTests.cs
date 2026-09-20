using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;

namespace Rateio.Application.Tests.Despesas;

/// <summary>
/// Cobre T28.2 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/> mockados (mesmo
/// padrão de <c>EditarDespesaUseCaseTests</c>): nenhum teste aqui toca EF/Postgres. Entregável da
/// task: exclusão respeita RNF07 (404 grupo inexistente, 403 sem acesso) e despesa inexistente
/// nesse grupo vira 404 via <see cref="DespesaNaoEncontradaException"/>.
/// </summary>
public class ExcluirDespesaUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();

    private ExcluirDespesaUseCase CriarUseCase() => new(_grupoRepository.Object, _despesaRepository.Object);

    [Fact]
    public async Task ExecutarAsync_DespesaExistenteEUsuarioComAcesso_Remove()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        _despesaRepository
            .Setup(r => r.RemoverAsync(grupoId, despesaId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(true);

        var useCase = CriarUseCase();

        await useCase.ExecutarAsync(donoUsuarioId, grupoId, despesaId);

        _despesaRepository.Verify(r => r.RemoverAsync(grupoId, despesaId, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemRemover()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId, despesaId));

        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.RemoverAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegadoSemRemover()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var usuarioSemAcessoId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId, despesaId));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.RemoverAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_DespesaNaoExisteNesseGrupo_LancaDespesaNaoEncontrada()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        _despesaRepository
            .Setup(r => r.RemoverAsync(grupoId, despesaId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<DespesaNaoEncontradaException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, despesaId));

        Assert.Equal(grupoId, excecao.GrupoId);
        Assert.Equal(despesaId, excecao.DespesaId);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
