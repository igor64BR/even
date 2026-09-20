using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Tests.Despesas;

/// <summary>
/// Cobre T28.1 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/> mockados (mesmo
/// padrão de <c>CriarDespesaUseCaseTests</c>): nenhum teste aqui toca EF/Postgres. Entregável da
/// task: edição válida reconstrói a despesa inteira e persiste, RNF07 respeitado (404 grupo
/// inexistente, 403 sem acesso), e despesa inexistente nesse grupo também vira 404 — via
/// <see cref="DespesaNaoEncontradaException"/>, mapeada pelo controller.
/// </summary>
public class EditarDespesaUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();

    private EditarDespesaUseCase CriarUseCase() => new(_grupoRepository.Object, _despesaRepository.Object);

    [Fact]
    public async Task ExecutarAsync_DespesaValidaEUsuarioComAcesso_ReconstroiAPersisteComIdDaRota()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        DespesaParaPersistir? capturada = null;
        var grupoIdRecebido = Guid.Empty;
        _despesaRepository
            .Setup(r => r.AtualizarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, DespesaParaPersistir, CancellationToken>((grupo, despesa, _) =>
            {
                grupoIdRecebido = grupo;
                capturada = despesa;
            })
            .ReturnsAsync(true);

        var useCase = CriarUseCase();
        // Requisição chega com um Id diferente do da rota — o da rota é que deve prevalecer (nunca
        // confiar em identidade vinda do corpo, mesmo cuidado de RNF07 pro dono do grupo).
        var requisicao = RequisicaoValida(Guid.NewGuid(), out var anaId, out var brunoId);

        await useCase.ExecutarAsync(donoUsuarioId, grupoId, despesaId, requisicao);

        Assert.Equal(grupoId, grupoIdRecebido);
        Assert.NotNull(capturada);
        Assert.Equal(despesaId, capturada!.Despesa.Id);
        Assert.Equal(requisicao.Descricao, capturada.Descricao);
        Assert.Equal(requisicao.Data, capturada.Data);
        Assert.Equal(anaId, capturada.Despesa.PagadorId.Valor);
        Assert.Equal(2, capturada.Despesa.Participacoes.Count);
        Assert.Contains(capturada.Despesa.Participacoes, p => p.ParticipanteId.Valor == brunoId);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(despesaId, out _, out _);

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId, despesaId, requisicao));

        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.AtualizarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegadoSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var usuarioSemAcessoId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(despesaId, out _, out _);

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId, despesaId, requisicao));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.AtualizarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_DespesaNaoExisteNesseGrupo_LancaDespesaNaoEncontrada()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        _despesaRepository
            .Setup(r => r.AtualizarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoValida(despesaId, out _, out _);

        var excecao = await Assert.ThrowsAsync<DespesaNaoEncontradaException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, despesaId, requisicao));

        Assert.Equal(grupoId, excecao.GrupoId);
        Assert.Equal(despesaId, excecao.DespesaId);
    }

    [Fact]
    public async Task ExecutarAsync_ParticipacaoPorValorFixoSemValor_FalhaSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var despesaId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var requisicaoInvalida = new DespesaSincronizadaRequest(
            despesaId,
            "Jantar",
            ValorTotalCentavos: 1000,
            PagadorId: anaId,
            Data: new DateOnly(2026, 1, 10),
            TipoDivisao: TipoDivisaoRequest.PorValorFixo,
            Participacoes:
            [
                new ParticipacaoSincronizadaRequest(anaId, Peso: null, ValorCentavos: null),
                new ParticipacaoSincronizadaRequest(brunoId, Peso: null, ValorCentavos: null),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, despesaId, requisicaoInvalida));

        _despesaRepository.Verify(
            r => r.AtualizarAsync(It.IsAny<Guid>(), It.IsAny<DespesaParaPersistir>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));

    private static DespesaSincronizadaRequest RequisicaoValida(Guid idNaRequisicao, out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new DespesaSincronizadaRequest(
            idNaRequisicao,
            "Jantar editado",
            ValorTotalCentavos: 2000,
            PagadorId: anaId,
            Data: new DateOnly(2026, 2, 1),
            TipoDivisao: TipoDivisaoRequest.PorIgual,
            Participacoes:
            [
                new ParticipacaoSincronizadaRequest(anaId, Peso: null, ValorCentavos: null),
                new ParticipacaoSincronizadaRequest(brunoId, Peso: null, ValorCentavos: null),
            ]);
    }
}
