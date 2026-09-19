using Moq;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Tests.Grupos;

/// <summary>
/// Cobre T18.1/T18.2 inteiramente com um <see cref="IGrupoRepository"/> mockado (mesmo padrão de
/// <c>AutenticarComGoogleUseCaseTests</c>): nenhum teste aqui toca EF/Postgres. A parte central —
/// "grupo com despesas sincroniza corretamente, saldo bate com o motor de T31" — é verificada
/// capturando o <see cref="GrupoParaSincronizar"/> que o use case passou pro repositório e
/// alimentando as despesas mapeadas no <see cref="MotorDeSimplificacaoDeDividas"/> real: se o
/// mapeamento DTO→Domínio (<see cref="ConstrutorDeGrupoSincronizado"/>) estiver certo, o saldo
/// recalculado bate com o valor esperado a mão.
/// </summary>
public class SincronizarGrupoUseCaseTests
{
    private static readonly Guid UsuarioAutenticadoId = Guid.NewGuid();

    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly IMotorDeSimplificacaoDeDividas _motor = new MotorDeSimplificacaoDeDividas();

    private SincronizarGrupoUseCase CriarUseCase() => new(_grupoRepository.Object);

    [Fact]
    public async Task ExecutarAsync_GrupoValido_PersisteComDonoIgualAoUsuarioAutenticado()
    {
        GrupoParaSincronizar? capturado = null;
        _grupoRepository
            .Setup(r => r.SincronizarAsync(It.IsAny<GrupoParaSincronizar>(), It.IsAny<CancellationToken>()))
            .Callback<GrupoParaSincronizar, CancellationToken>((grupo, _) => capturado = grupo)
            .ReturnsAsync(Guid.NewGuid());

        var useCase = CriarUseCase();
        var requisicao = RequisicaoComUmaDespesaPorIgual(out _, out _);

        await useCase.ExecutarAsync(UsuarioAutenticadoId, requisicao);

        Assert.NotNull(capturado);
        Assert.Equal(UsuarioAutenticadoId, capturado!.DonoUsuarioId);
        Assert.Equal(requisicao.Nome, capturado.Grupo.Nome.Valor);
        Assert.Equal(requisicao.Categoria, capturado.Grupo.Categoria);
        Assert.True(capturado.Grupo.Sincronizado, "T18.2: grupo sincronizado precisa virar fonte da verdade.");
        Assert.Equal(2, capturado.Grupo.Participantes.Count);
        Assert.Single(capturado.Despesas);
    }

    [Fact]
    public async Task ExecutarAsync_RetornaOIdDoGrupoDevolvidoPeloRepositorio()
    {
        var idDoServidor = Guid.NewGuid();
        _grupoRepository
            .Setup(r => r.SincronizarAsync(It.IsAny<GrupoParaSincronizar>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(idDoServidor);

        var useCase = CriarUseCase();
        var requisicao = RequisicaoComUmaDespesaPorIgual(out _, out _);

        var resultado = await useCase.ExecutarAsync(UsuarioAutenticadoId, requisicao);

        Assert.Equal(idDoServidor, resultado);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoComDespesaPorIgual_SaldoRecalculadoPeloMotorBateComOEsperado()
    {
        GrupoParaSincronizar? capturado = null;
        _grupoRepository
            .Setup(r => r.SincronizarAsync(It.IsAny<GrupoParaSincronizar>(), It.IsAny<CancellationToken>()))
            .Callback<GrupoParaSincronizar, CancellationToken>((grupo, _) => capturado = grupo)
            .ReturnsAsync(Guid.NewGuid());

        var useCase = CriarUseCase();
        // Ana paga 1000 centavos, dividido igualmente entre Ana e Bruno: cada um fica com metade —
        // exatamente o "case-01-simples" do motor (algorithm-spec.md), só que construído a partir
        // do payload em vez de um Despesa criado à mão.
        var requisicao = RequisicaoComUmaDespesaPorIgual(out var anaId, out var brunoId);

        await useCase.ExecutarAsync(UsuarioAutenticadoId, requisicao);

        var despesasMapeadas = capturado!.Despesas.Select(d => d.Despesa).ToList();
        var saldos = _motor.ComputeBalances(despesasMapeadas, quitacoes: []);

        Assert.Equal(Dinheiro.EmCentavos(500), saldos[new ParticipanteId(anaId)]);
        Assert.Equal(Dinheiro.EmCentavos(-500), saldos[new ParticipanteId(brunoId)]);

        var transacoes = _motor.ComputeSettlement(saldos);
        Assert.Equal(
            new[] { new Transacao(new ParticipanteId(brunoId), new ParticipanteId(anaId), Dinheiro.EmCentavos(500)) },
            transacoes);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoComDivisaoPorPesoEValorFixo_MapeiaOsDoisTiposCorretamente()
    {
        GrupoParaSincronizar? capturado = null;
        _grupoRepository
            .Setup(r => r.SincronizarAsync(It.IsAny<GrupoParaSincronizar>(), It.IsAny<CancellationToken>()))
            .Callback<GrupoParaSincronizar, CancellationToken>((grupo, _) => capturado = grupo)
            .ReturnsAsync(Guid.NewGuid());

        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var carlaId = Guid.NewGuid();
        var requisicao = new SincronizarGrupoRequest(
            "Viagem",
            CategoriaGrupo.Viagem,
            Participantes:
            [
                new ParticipanteSincronizadoRequest(anaId, "Ana", EhConvidado: false),
                new ParticipanteSincronizadoRequest(brunoId, "Bruno", EhConvidado: true),
                new ParticipanteSincronizadoRequest(carlaId, "Carla", EhConvidado: true),
            ],
            Despesas:
            [
                new DespesaSincronizadaRequest(
                    Guid.NewGuid(),
                    "Hospedagem",
                    ValorTotalCentavos: 3000,
                    PagadorId: anaId,
                    Data: new DateOnly(2026, 1, 10),
                    TipoDivisao: TipoDivisaoRequest.PorPeso,
                    Participacoes:
                    [
                        new ParticipacaoSincronizadaRequest(anaId, Peso: 2, ValorCentavos: null),
                        new ParticipacaoSincronizadaRequest(brunoId, Peso: 1, ValorCentavos: null),
                        new ParticipacaoSincronizadaRequest(carlaId, Peso: 1, ValorCentavos: null),
                    ]),
                new DespesaSincronizadaRequest(
                    Guid.NewGuid(),
                    "Pedágio",
                    ValorTotalCentavos: 900,
                    PagadorId: brunoId,
                    Data: new DateOnly(2026, 1, 11),
                    TipoDivisao: TipoDivisaoRequest.PorValorFixo,
                    Participacoes:
                    [
                        new ParticipacaoSincronizadaRequest(anaId, Peso: null, ValorCentavos: 500),
                        new ParticipacaoSincronizadaRequest(brunoId, Peso: null, ValorCentavos: 400),
                    ]),
            ]);

        var useCase = CriarUseCase();

        await useCase.ExecutarAsync(UsuarioAutenticadoId, requisicao);

        var despesasMapeadas = capturado!.Despesas.Select(d => d.Despesa).ToList();
        var saldos = _motor.ComputeBalances(despesasMapeadas, quitacoes: []);

        // Soma de todo saldo é sempre zero, independente do mix de tipos de divisão (invariante
        // do motor, T31).
        Assert.Equal(0, saldos.Values.Sum(v => v.Centavos));

        var hospedagem = despesasMapeadas.Single(d => d.ValorTotal == Dinheiro.EmCentavos(3000));
        Assert.All(hospedagem.Participacoes, p => Assert.IsType<ParticipacaoDespesa.PorPeso>(p));

        var pedagio = despesasMapeadas.Single(d => d.ValorTotal == Dinheiro.EmCentavos(900));
        Assert.All(pedagio.Participacoes, p => Assert.IsType<ParticipacaoDespesa.PorValorFixo>(p));
    }

    [Fact]
    public async Task ExecutarAsync_NomeDeGrupoVazio_FalhaSemChamarOsRepositorio()
    {
        var useCase = CriarUseCase();
        var requisicaoInvalida = RequisicaoComUmaDespesaPorIgual(out _, out _) with { Nome = "   " };

        await Assert.ThrowsAsync<ArgumentException>(() => useCase.ExecutarAsync(UsuarioAutenticadoId, requisicaoInvalida));

        _grupoRepository.Verify(
            r => r.SincronizarAsync(It.IsAny<GrupoParaSincronizar>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoSemParticipante_FalhaPorReaproveitarAInvarianteDeGrupoCriar()
    {
        var useCase = CriarUseCase();
        var requisicaoSemParticipantes = new SincronizarGrupoRequest(
            "Viagem",
            CategoriaGrupo.Viagem,
            Participantes: [],
            Despesas: []);

        var excecao = await Assert.ThrowsAsync<InvalidOperationException>(
            () => useCase.ExecutarAsync(UsuarioAutenticadoId, requisicaoSemParticipantes));

        Assert.Contains("pelo menos 1 participante", excecao.Message);
    }

    [Fact]
    public async Task ExecutarAsync_ParticipacaoPorPesoSemPeso_Falha()
    {
        var useCase = CriarUseCase();
        var anaId = Guid.NewGuid();
        var brunoId = Guid.NewGuid();
        var requisicao = new SincronizarGrupoRequest(
            "Viagem",
            CategoriaGrupo.Viagem,
            Participantes:
            [
                new ParticipanteSincronizadoRequest(anaId, "Ana", EhConvidado: false),
                new ParticipanteSincronizadoRequest(brunoId, "Bruno", EhConvidado: true),
            ],
            Despesas:
            [
                new DespesaSincronizadaRequest(
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
                    ]),
            ]);

        await Assert.ThrowsAsync<ArgumentException>(() => useCase.ExecutarAsync(UsuarioAutenticadoId, requisicao));
    }

    private static SincronizarGrupoRequest RequisicaoComUmaDespesaPorIgual(out Guid anaId, out Guid brunoId)
    {
        anaId = Guid.NewGuid();
        brunoId = Guid.NewGuid();

        return new SincronizarGrupoRequest(
            "Viagem",
            CategoriaGrupo.Viagem,
            Participantes:
            [
                new ParticipanteSincronizadoRequest(anaId, "Ana", EhConvidado: false),
                new ParticipanteSincronizadoRequest(brunoId, "Bruno", EhConvidado: true),
            ],
            Despesas:
            [
                new DespesaSincronizadaRequest(
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
                    ]),
            ]);
    }
}
