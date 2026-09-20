using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;
using Rateio.Application.Quitacoes;
using Rateio.Application.Simplificacao;
using Rateio.Domain;

namespace Rateio.Application.Tests.Quitacoes;

/// <summary>
/// T35.2: prova o critério de entregável que os testes isolados de
/// <c>RegistrarQuitacaoUseCaseTests</c> e <c>ObterSimplificacaoDeDividasUseCaseTests</c> (T32) não
/// cobrem sozinhos — que uma quitação registrada por <see cref="RegistrarQuitacaoUseCase"/>
/// (T35.1) é lida de volta por <see cref="ObterSimplificacaoDeDividasUseCase"/> (T32) na próxima
/// chamada, sem precisar de saldo em cache. Usa um <see cref="IQuitacaoRepository"/> falso e simples
/// (lista em memória) em vez de mock, porque o que este teste precisa é estado real persistindo
/// entre as duas chamadas de caso de uso — um mock de <c>AdicionarAsync</c> não alimentaria de volta
/// <c>ObterPorGrupoAsync</c>. Não toca EF/Postgres: é o mesmo nível de "unit test do caso de uso"
/// dos demais arquivos desta pasta, só com uma dependência falsa em vez de mockada.
/// </summary>
public class RegistrarQuitacaoRefletindoNoSettlementTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();
    private readonly QuitacaoRepositoryEmMemoria _quitacaoRepository = new();
    private readonly IMotorDeSimplificacaoDeDividas _motor = new MotorDeSimplificacaoDeDividas();

    [Fact]
    public async Task QuitacaoRegistrada_ReduzOValorNaProximaChamadaDeObterSimplificacao()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var a = new ParticipanteId(Guid.NewGuid());
        var b = new ParticipanteId(Guid.NewGuid());
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));

        // A deve 1000 pra B.
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: b,
            Participacoes: [new ParticipacaoDespesa.PorValorFixo(a, Dinheiro.EmCentavos(1000))]);
        _despesaRepository
            .Setup(r => r.ObterPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { despesa });

        var obterSimplificacao = new ObterSimplificacaoDeDividasUseCase(
            _grupoRepository.Object, _despesaRepository.Object, _quitacaoRepository, _motor);

        var antesDaQuitacao = await obterSimplificacao.ExecutarAsync(donoUsuarioId, grupoId);
        Assert.Equal([new Transacao(a, b, Dinheiro.EmCentavos(1000))], antesDaQuitacao);

        // T35.1: A paga 400 a B via POST /groups/{id}/settlements.
        var registrarQuitacao = new RegistrarQuitacaoUseCase(
            _grupoRepository.Object, _quitacaoRepository, Mock.Of<INotificadorDeEventoDeGrupo>());
        var requisicao = new RegistrarQuitacaoRequest(a.Valor, b.Valor, ValorCentavos: 400);
        await registrarQuitacao.ExecutarAsync(donoUsuarioId, grupoId, requisicao);

        var depoisDaQuitacao = await obterSimplificacao.ExecutarAsync(donoUsuarioId, grupoId);

        Assert.Equal([new Transacao(a, b, Dinheiro.EmCentavos(600))], depoisDaQuitacao);
    }

    /// <summary>
    /// Fake mínimo de <see cref="IQuitacaoRepository"/> — só o suficiente pra este teste encadear os
    /// dois casos de uso contra o mesmo estado, sem trazer EF Core pra um teste de Application.
    /// </summary>
    private sealed class QuitacaoRepositoryEmMemoria : IQuitacaoRepository
    {
        private readonly Dictionary<Guid, List<Quitacao>> _porGrupo = [];

        public Task<IReadOnlyList<Quitacao>> ObterPorGrupoAsync(
            Guid grupoId, CancellationToken cancellationToken = default) =>
            Task.FromResult<IReadOnlyList<Quitacao>>(
                _porGrupo.TryGetValue(grupoId, out var quitacoes) ? quitacoes : []);

        public Task AdicionarAsync(Guid grupoId, Quitacao quitacao, CancellationToken cancellationToken = default)
        {
            if (!_porGrupo.TryGetValue(grupoId, out var quitacoes))
            {
                quitacoes = [];
                _porGrupo[grupoId] = quitacoes;
            }

            quitacoes.Add(quitacao);
            return Task.CompletedTask;
        }

        // T39.1: este fake cobre só o fluxo de settlement (ObterPorGrupoAsync/AdicionarAsync) que
        // este teste exercita — o fallback de pull tem cobertura própria em
        // Notificacoes.ObterEventosDeGrupoUseCaseTests, com um repositório mockado (Moq), não este
        // fake em memória.
        public Task<IReadOnlyList<QuitacaoOcorrida>> ObterOcorridasDesdeAsync(
            Guid grupoId, DateTimeOffset desde, CancellationToken cancellationToken = default) =>
            throw new NotSupportedException(
                $"{nameof(QuitacaoRepositoryEmMemoria)} não implementa {nameof(ObterOcorridasDesdeAsync)} — fora do escopo deste teste.");
    }
}
