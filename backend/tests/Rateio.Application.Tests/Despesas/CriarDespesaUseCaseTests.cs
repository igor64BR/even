using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Tests.Despesas;

/// <summary>
/// Cobre T23.2 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/> mockados (mesmo
/// padrão de <c>SincronizarGrupoUseCaseTests</c>): nenhum teste aqui toca EF/Postgres. As três
/// regras do entregável da task — despesa válida persiste, grupo inexistente vira
/// <see cref="GrupoNaoEncontradoException"/> (o controller mapeia pra 404), usuário sem acesso vira
/// <see cref="AcessoNegadoException"/> (controller mapeia pra 403) — cada uma tem seu teste próprio.
/// </summary>
public class CriarDespesaUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();

    private CriarDespesaUseCase CriarUseCase() => new(_grupoRepository.Object, _despesaRepository.Object);

    [Fact]
    public async Task ExecutarAsync_DespesaValidaEUsuarioComAcesso_PersisteNoGrupoCorreto()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        DespesaParaPersistir? capturada = null;
        var grupoIdRecebido = Guid.Empty;
        _despesaRepository
            .Setup(r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, DespesaParaPersistir, CancellationToken>((grupo, despesa, _) =>
            {
                grupoIdRecebido = grupo;
                capturada = despesa;
            })
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(out var anaId, out var brunoId);

        var despesaId = await useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        Assert.Equal(requisicao.Id, despesaId);
        Assert.Equal(grupoId, grupoIdRecebido);
        Assert.NotNull(capturada);
        Assert.Equal(requisicao.Descricao, capturada!.Descricao);
        Assert.Equal(requisicao.Data, capturada.Data);
        Assert.Equal(anaId, capturada.Despesa.PagadorId.Valor);
        Assert.Equal(2, capturada.Despesa.Participacoes.Count);
        Assert.All(capturada.Despesa.Participacoes, p => Assert.IsType<ParticipacaoDespesa.PorIgual>(p));
        // Confirma que os dois participantes da requisição foram mapeados, não só o pagador.
        Assert.Contains(capturada.Despesa.Participacoes, p => p.ParticipanteId.Valor == brunoId);

        _despesaRepository.Verify(
            r => r.AdicionarAsync(grupoId, It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(out _, out _);

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId, requisicao));

        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegadoSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var usuarioSemAcessoId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(out _, out _);

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId, requisicao));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_DonoDoGrupo_TemAcessoMesmoSemSerParticipanteListado()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        _despesaRepository
            .Setup(r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(out _, out _);

        await useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        _despesaRepository.Verify(
            r => r.AdicionarAsync(grupoId, It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_ParticipacaoPorPesoSemPeso_FalhaSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var requisicaoInvalida = new DespesaSincronizadaRequest(
            Guid.NewGuid(),
            "Jantar",
            ValorTotalCentavos: 1000,
            PagadorId: anaId,
            Data: new DateOnly(2026, 1, 10),
            TipoDivisao: TipoDivisaoRequest.PorPeso,
            Participacoes:
            [
                new ParticipacaoSincronizadaRequest(anaId, Peso: null, ValorCentavos: null),
                new ParticipacaoSincronizadaRequest(brunoId, Peso: null, ValorCentavos: null),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicaoInvalida));

        _despesaRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));

    private static DespesaSincronizadaRequest RequisicaoValida(out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new DespesaSincronizadaRequest(
            Guid.NewGuid(),
            "Jantar",
            ValorTotalCentavos: 1000,
            PagadorId: anaId,
            Data: new DateOnly(2026, 1, 10),
            TipoDivisao: TipoDivisaoRequest.PorIgual,
            Participacoes:
            [
                new ParticipacaoSincronizadaRequest(anaId, Peso: null, ValorCentavos: null),
                new ParticipacaoSincronizadaRequest(brunoId, Peso: null, ValorCentavos: null),
            ]);
    }
}
