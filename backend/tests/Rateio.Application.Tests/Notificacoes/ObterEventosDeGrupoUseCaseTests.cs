using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;
using Rateio.Application.Quitacoes;

namespace Rateio.Application.Tests.Notificacoes;

/// <summary>
/// Cobre T39.1 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/>/
/// <see cref="IQuitacaoRepository"/> mockados (mesmo padrão de
/// <c>Simplificacao.ObterSimplificacaoDeDividasUseCaseTests</c>, T32) — nenhum teste aqui toca
/// EF/Postgres. O entregável da task: eventos depois de "desde" retornam corretos e ordenados
/// (despesas e quitações intercaladas por <c>CriadoEm</c>), grupo sem eventos novos devolve lista
/// vazia, e RNF07 é respeitado (grupo inexistente/sem acesso lança antes de consultar despesas ou
/// quitações) — cada um tem seu teste próprio.
/// </summary>
public class ObterEventosDeGrupoUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();
    private readonly Mock<IQuitacaoRepository> _quitacaoRepository = new();

    private ObterEventosDeGrupoUseCase CriarUseCase() =>
        new(_grupoRepository.Object, _despesaRepository.Object, _quitacaoRepository.Object);

    [Fact]
    public async Task ExecutarAsync_DespesasEQuitacoesDepoisDeDesde_RetornaEventosIntercaladosEOrdenadosPorCriadoEm()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        var desde = new DateTimeOffset(2026, 9, 19, 12, 0, 0, TimeSpan.Zero);

        var pagadorDespesaId = Guid.NewGuid();
        var despesaMaisRecente = new DespesaOcorrida(
            Guid.NewGuid(), "Jantar", 5000, pagadorDespesaId, desde.AddMinutes(30));
        var despesaMaisAntiga = new DespesaOcorrida(
            Guid.NewGuid(), "Mercado", 3000, pagadorDespesaId, desde.AddMinutes(10));

        var deId = Guid.NewGuid();
        var paraId = Guid.NewGuid();
        var quitacaoDoMeio = new QuitacaoOcorrida(
            Guid.NewGuid(), deId, paraId, 1200, desde.AddMinutes(20));

        _despesaRepository
            .Setup(r => r.ObterOcorridasDesdeAsync(grupoId, desde, It.IsAny<CancellationToken>()))
            .ReturnsAsync([despesaMaisRecente, despesaMaisAntiga]);
        _quitacaoRepository
            .Setup(r => r.ObterOcorridasDesdeAsync(grupoId, desde, It.IsAny<CancellationToken>()))
            .ReturnsAsync([quitacaoDoMeio]);

        var useCase = CriarUseCase();

        var eventos = await useCase.ExecutarAsync(donoUsuarioId, grupoId, desde);

        Assert.Equal(3, eventos.Count);

        var primeiro = Assert.IsType<EventoDespesaCriada>(eventos[0]);
        Assert.Equal(despesaMaisAntiga.Id, primeiro.DespesaId);
        Assert.Equal("Mercado", primeiro.Descricao);
        Assert.Equal(3000, primeiro.ValorTotalCentavos);
        Assert.Equal(pagadorDespesaId, primeiro.PagadorId);
        Assert.Equal(grupoId, primeiro.GrupoId);

        var segundo = Assert.IsType<EventoDividaQuitada>(eventos[1]);
        Assert.Equal(quitacaoDoMeio.Id, segundo.QuitacaoId);
        Assert.Equal(deId, segundo.DeParticipanteId);
        Assert.Equal(paraId, segundo.ParaParticipanteId);
        Assert.Equal(1200, segundo.ValorCentavos);
        Assert.Equal(grupoId, segundo.GrupoId);

        var terceiro = Assert.IsType<EventoDespesaCriada>(eventos[2]);
        Assert.Equal(despesaMaisRecente.Id, terceiro.DespesaId);
        Assert.Equal("Jantar", terceiro.Descricao);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoSemEventosNovos_RetornaListaVazia()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        var desde = DateTimeOffset.UtcNow;

        _despesaRepository
            .Setup(r => r.ObterOcorridasDesdeAsync(grupoId, desde, It.IsAny<CancellationToken>()))
            .ReturnsAsync([]);
        _quitacaoRepository
            .Setup(r => r.ObterOcorridasDesdeAsync(grupoId, desde, It.IsAny<CancellationToken>()))
            .ReturnsAsync([]);

        var useCase = CriarUseCase();

        var eventos = await useCase.ExecutarAsync(donoUsuarioId, grupoId, desde);

        Assert.Empty(eventos);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemConsultarDespesasOuQuitacoes()
    {
        var grupoId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId, DateTimeOffset.UtcNow));

        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.ObterOcorridasDesdeAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _quitacaoRepository.Verify(
            r => r.ObterOcorridasDesdeAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegadoSemConsultarDespesasOuQuitacoes()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var usuarioSemAcessoId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId, DateTimeOffset.UtcNow));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.ObterOcorridasDesdeAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _quitacaoRepository.Verify(
            r => r.ObterOcorridasDesdeAsync(It.IsAny<Guid>(), It.IsAny<DateTimeOffset>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
