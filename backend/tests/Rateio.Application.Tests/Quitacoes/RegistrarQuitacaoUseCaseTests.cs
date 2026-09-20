using Moq;
using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;
using Rateio.Application.Quitacoes;
using Rateio.Domain;

namespace Rateio.Application.Tests.Quitacoes;

/// <summary>
/// Cobre T35.1 com <see cref="IGrupoRepository"/>/<see cref="IQuitacaoRepository"/> mockados (mesmo
/// padrão de <c>CriarDespesaUseCaseTests</c>, T23): nenhum teste aqui toca EF/Postgres. As regras do
/// entregável da task — quitação válida persiste no grupo certo, grupo inexistente vira
/// <see cref="GrupoNaoEncontradoException"/> (o controller mapeia pra 404), usuário sem acesso vira
/// <see cref="AcessoNegadoException"/> (controller mapeia pra 403), payload inválido nunca persiste —
/// cada uma tem seu teste próprio. "Reflete na próxima chamada a GET /groups/{id}/settlement" (o
/// outro critério do entregável) é coberto separadamente por
/// <c>RegistrarQuitacaoRefletindoNoSettlementTests</c>, que exercita os dois casos de uso encadeados
/// contra o mesmo repositório em memória. T38.3 acrescenta a cobertura de
/// <see cref="INotificadorDeEventoDeGrupo"/>: mockado aqui (sem Hub real), com um teste próprio
/// confirmando que o evento certo é disparado após persistir.
/// </summary>
public class RegistrarQuitacaoUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IQuitacaoRepository> _quitacaoRepository = new();
    private readonly Mock<INotificadorDeEventoDeGrupo> _notificadorDeEventoDeGrupo = new();

    private RegistrarQuitacaoUseCase CriarUseCase() =>
        new(_grupoRepository.Object, _quitacaoRepository.Object, _notificadorDeEventoDeGrupo.Object);

    [Fact]
    public async Task ExecutarAsync_QuitacaoValidaEUsuarioComAcesso_PersisteNoGrupoCorreto()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        Quitacao? capturada = null;
        var grupoIdRecebido = Guid.Empty;
        _quitacaoRepository
            .Setup(r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, Quitacao, CancellationToken>((grupo, quitacao, _) =>
            {
                grupoIdRecebido = grupo;
                capturada = quitacao;
            })
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var requisicao = new RegistrarQuitacaoRequest(anaId, brunoId, ValorCentavos: 400);

        var quitacaoId = await useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        Assert.Equal(grupoId, grupoIdRecebido);
        Assert.NotNull(capturada);
        Assert.Equal(quitacaoId, capturada!.Id);
        Assert.Equal(anaId, capturada.PagadorId.Valor);
        Assert.Equal(brunoId, capturada.RecebedorId.Valor);
        Assert.Equal(400, capturada.Valor.Centavos);

        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(grupoId, It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_QuitacaoValidaEUsuarioComAcesso_NotificaEventoDividaQuitadaAposPersistir()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var ordemDasChamadas = new List<string>();
        _quitacaoRepository
            .Setup(r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()))
            .Callback(() => ordemDasChamadas.Add("persistiu"))
            .Returns(Task.CompletedTask);

        IEventoDeGrupo? eventoNotificado = null;
        _notificadorDeEventoDeGrupo
            .Setup(n => n.NotificarAsync(It.IsAny<IEventoDeGrupo>(), It.IsAny<CancellationToken>()))
            .Callback<IEventoDeGrupo, CancellationToken>((evento, _) =>
            {
                ordemDasChamadas.Add("notificou");
                eventoNotificado = evento;
            })
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var requisicao = new RegistrarQuitacaoRequest(anaId, brunoId, ValorCentavos: 400);

        var quitacaoId = await useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        // A notificação só faz sentido depois que a quitação já está persistida com sucesso.
        Assert.Equal(["persistiu", "notificou"], ordemDasChamadas);

        var eventoDividaQuitada = Assert.IsType<EventoDividaQuitada>(eventoNotificado);
        Assert.Equal(grupoId, eventoDividaQuitada.GrupoId);
        Assert.Equal(quitacaoId, eventoDividaQuitada.QuitacaoId);
        Assert.Equal(anaId, eventoDividaQuitada.DeParticipanteId);
        Assert.Equal(brunoId, eventoDividaQuitada.ParaParticipanteId);
        Assert.Equal(400, eventoDividaQuitada.ValorCentavos);
        Assert.Equal(TipoEventoDeGrupo.DividaQuitada, eventoDividaQuitada.Tipo);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();
        var requisicao = new RegistrarQuitacaoRequest(Guid.NewGuid(), Guid.NewGuid(), ValorCentavos: 400);

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId, requisicao));

        Assert.Equal(grupoId, excecao.GrupoId);
        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _notificadorDeEventoDeGrupo.Verify(
            n => n.NotificarAsync(It.IsAny<IEventoDeGrupo>(), It.IsAny<CancellationToken>()),
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
        var requisicao = new RegistrarQuitacaoRequest(Guid.NewGuid(), Guid.NewGuid(), ValorCentavos: 400);

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId, requisicao));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _notificadorDeEventoDeGrupo.Verify(
            n => n.NotificarAsync(It.IsAny<IEventoDeGrupo>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_DonoDoGrupo_TemAcessoMesmoSemSerParticipanteListado()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);
        _quitacaoRepository
            .Setup(r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();
        var requisicao = new RegistrarQuitacaoRequest(Guid.NewGuid(), Guid.NewGuid(), ValorCentavos: 400);

        await useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(grupoId, It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-100)]
    public async Task ExecutarAsync_ValorNaoPositivo_FalhaSemPersistir(long valorCentavos)
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var requisicao = new RegistrarQuitacaoRequest(Guid.NewGuid(), Guid.NewGuid(), valorCentavos);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao));

        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _notificadorDeEventoDeGrupo.Verify(
            n => n.NotificarAsync(It.IsAny<IEventoDeGrupo>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_PagadorIgualAoRecebedor_FalhaSemPersistir()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var participanteId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();
        var requisicao = new RegistrarQuitacaoRequest(participanteId, participanteId, ValorCentavos: 400);

        await Assert.ThrowsAsync<ArgumentException>(
            () => useCase.ExecutarAsync(donoUsuarioId, grupoId, requisicao));

        _quitacaoRepository.Verify(
            r => r.AdicionarAsync(It.IsAny<Guid>(), It.IsAny<Quitacao>(), It.IsAny<CancellationToken>()),
            Times.Never);
        _notificadorDeEventoDeGrupo.Verify(
            n => n.NotificarAsync(It.IsAny<IEventoDeGrupo>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
