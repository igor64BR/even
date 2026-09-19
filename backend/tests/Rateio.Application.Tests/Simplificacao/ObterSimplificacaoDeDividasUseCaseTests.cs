using System.Diagnostics;
using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Quitacoes;
using Rateio.Application.Simplificacao;
using Rateio.Domain;

namespace Rateio.Application.Tests.Simplificacao;

/// <summary>
/// Cobre T32.1 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/>/
/// <see cref="IQuitacaoRepository"/> mockados (mesmo padrão de <c>CriarDespesaUseCaseTests</c>, T23)
/// — nenhum teste aqui toca EF/Postgres. O motor (<see cref="MotorDeSimplificacaoDeDividas"/>, T31)
/// é usado de verdade, não mockado: o que este caso de uso precisa provar é que ele orquestra
/// certo (carrega despesas+quitações do grupo certo, só depois de validar acesso, e repassa pro
/// motor sem alterar o resultado) — mockar o motor esconderia justamente isso.
/// </summary>
public class ObterSimplificacaoDeDividasUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();
    private readonly Mock<IQuitacaoRepository> _quitacaoRepository = new();
    private readonly IMotorDeSimplificacaoDeDividas _motor = new MotorDeSimplificacaoDeDividas();

    private ObterSimplificacaoDeDividasUseCase CriarUseCase() =>
        new(_grupoRepository.Object, _despesaRepository.Object, _quitacaoRepository.Object, _motor);

    /// <summary>
    /// Fixture = case-01-simples de algorithm-spec.md, montada a partir de uma despesa real (não
    /// dos saldos já prontos, diferente de como o teste de T31 exercita esse caso) — B paga R$10,00
    /// e A é o único participante da divisão, então computeBalances chega em A:-1000/B:+1000
    /// antes mesmo de chamar computeSettlement.
    /// </summary>
    [Fact]
    public async Task ExecutarAsync_GrupoComDespesas_RetornaSimplificacaoDeCase01Simples()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var a = new ParticipanteId(Guid.NewGuid());
        var b = new ParticipanteId(Guid.NewGuid());
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: b,
            Participacoes: [new ParticipacaoDespesa.PorValorFixo(a, Dinheiro.EmCentavos(1000))]);
        _despesaRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { despesa });
        _quitacaoRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(Array.Empty<Quitacao>());

        var useCase = CriarUseCase();

        var transacoes = await useCase.ExecutarAsync(donoUsuarioId, grupoId);

        var esperado = new[] { new Transacao(a, b, Dinheiro.EmCentavos(1000)) };
        Assert.Equal(esperado, transacoes);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoComDespesaEQuitacaoParcial_ReduzAValorRestante()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var a = new ParticipanteId(Guid.NewGuid());
        var b = new ParticipanteId(Guid.NewGuid());
        ConfigurarAcesso(grupoId, donoUsuarioId);

        // A deve 1000 pra B; A já quitou 400 dessa dívida -> falta simplificar só 600.
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: b,
            Participacoes: [new ParticipacaoDespesa.PorValorFixo(a, Dinheiro.EmCentavos(1000))]);
        var quitacao = new Quitacao(Guid.NewGuid(), PagadorId: a, RecebedorId: b, Dinheiro.EmCentavos(400));
        _despesaRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { despesa });
        _quitacaoRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { quitacao });

        var useCase = CriarUseCase();

        var transacoes = await useCase.ExecutarAsync(donoUsuarioId, grupoId);

        var esperado = new[] { new Transacao(a, b, Dinheiro.EmCentavos(600)) };
        Assert.Equal(esperado, transacoes);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontradoSemLerDespesas()
    {
        var grupoId = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((AcessoAoGrupo?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync(Guid.NewGuid(), grupoId));

        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.ObterPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
        _quitacaoRepository.Verify(
            r => r.ObterPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegadoSemLerDespesas()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var usuarioSemAcessoId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<AcessoNegadoException>(
            () => useCase.ExecutarAsync(usuarioSemAcessoId, grupoId));

        Assert.Equal(usuarioSemAcessoId, excecao.UsuarioId);
        Assert.Equal(grupoId, excecao.GrupoId);
        _despesaRepository.Verify(
            r => r.ObterPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
        _quitacaoRepository.Verify(
            r => r.ObterPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    /// <summary>
    /// RNF02 (spec.md): a simplificação de um grupo de até 50 participantes completa em menos de
    /// 200ms. A task assumia isso já coberto pelos testes de T31, mas
    /// <c>Rateio.Domain.Tests.MotorDeSimplificacaoDeDividasTests</c> não tem nenhum teste de tempo —
    /// só os cinco casos de algorithm-spec.md e as variações de tipo de divisão. Fica coberto aqui,
    /// no nível do caso de uso completo (mais representativo do orçamento real do endpoint do que
    /// medir só o motor isolado).
    /// </summary>
    [Fact]
    public async Task ExecutarAsync_GrupoComCinquentaParticipantes_CompletaEmMenosDe200Ms()
    {
        const int quantidadeParticipantes = 50;
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var participantes = Enumerable.Range(0, quantidadeParticipantes)
            .Select(_ => new ParticipanteId(Guid.NewGuid()))
            .ToList();

        // Cada participante paga uma despesa de R$100,00 dividida igualmente entre todo mundo —
        // gera saldo positivo e negativo espalhado entre os 50, o cenário mais custoso pro guloso
        // (fila cheia nos dois lados) em vez de um caso já quase resolvido.
        var despesas = participantes
            .Select(pagador => new Despesa(
                Guid.NewGuid(),
                Dinheiro.EmCentavos(10000),
                PagadorId: pagador,
                Participacoes: participantes.Select(p => (ParticipacaoDespesa)new ParticipacaoDespesa.PorIgual(p)).ToList()))
            .ToList();

        _despesaRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(despesas);
        _quitacaoRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(Array.Empty<Quitacao>());

        var useCase = CriarUseCase();

        var cronometro = Stopwatch.StartNew();
        await useCase.ExecutarAsync(donoUsuarioId, grupoId);
        cronometro.Stop();

        Assert.True(
            cronometro.ElapsedMilliseconds < 200,
            $"RNF02 violado: {cronometro.ElapsedMilliseconds}ms para {quantidadeParticipantes} participantes (limite: 200ms).");
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
