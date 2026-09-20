using Moq;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Tests.Grupos;

/// <summary>
/// Cobre T28.3 com <see cref="IGrupoRepository"/>/<see cref="IDespesaRepository"/> mockados (mesmo
/// padrão de <c>CriarDespesaUseCaseTests</c>): nenhum teste aqui toca EF/Postgres. Entregável da
/// task: grupo existente e com acesso devolve nome/categoria/participantes/despesas juntos
/// (<see cref="GrupoCompleto"/>), RNF07 respeitado (404 grupo inexistente, 403 sem acesso) — a
/// lacuna reportada por T22 (entrar via link só devolve o id do grupo).
/// </summary>
public class ObterGrupoUseCaseTests
{
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly Mock<IDespesaRepository> _despesaRepository = new();

    private ObterGrupoUseCase CriarUseCase() => new(_grupoRepository.Object, _despesaRepository.Object);

    [Fact]
    public async Task ExecutarAsync_GrupoExistenteEUsuarioComAcesso_DevolveParticipantesEDespesas()
    {
        var grupoId = Guid.NewGuid();
        var donoUsuarioId = Guid.NewGuid();
        var anaId = Guid.NewGuid();
        ConfigurarAcesso(grupoId, donoUsuarioId);

        var anaParticipante = Participante.Autenticado(new ParticipanteId(anaId), NomeParticipante.Criar("Ana"));
        var grupoParaEntrada = new GrupoParaEntrada(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [anaParticipante]);
        _grupoRepository
            .Setup(r => r.ObterParaEntradaAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(grupoParaEntrada);

        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1500),
            new ParticipanteId(anaId),
            [new ParticipacaoDespesa.PorIgual(new ParticipanteId(anaId))]);
        var despesaParaPersistir = new DespesaParaPersistir(despesa, "Jantar", new DateOnly(2026, 1, 10));
        _despesaRepository
            .Setup(r => r.ObterDetalhadasPorGrupoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync([despesaParaPersistir]);

        var useCase = CriarUseCase();

        var resultado = await useCase.ExecutarAsync(donoUsuarioId, grupoId);

        Assert.Equal("Viagem", resultado.Grupo.Nome.Valor);
        Assert.Equal(CategoriaGrupo.Viagem, resultado.Grupo.Categoria);
        Assert.Single(resultado.Grupo.Participantes);
        Assert.Equal(anaId, resultado.Grupo.Participantes[0].Id.Valor);
        Assert.Single(resultado.Despesas);
        Assert.Equal("Jantar", resultado.Despesas[0].Descricao);
        Assert.Equal(despesa.Id, resultado.Despesas[0].Despesa.Id);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoInexistente_LancaGrupoNaoEncontrado()
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
            r => r.ObterDetalhadasPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_UsuarioSemAcessoAoGrupo_LancaAcessoNegado()
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
            r => r.ObterDetalhadasPorGrupoAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    private void ConfigurarAcesso(Guid grupoId, Guid donoUsuarioId) =>
        _grupoRepository
            .Setup(r => r.ObterAcessoAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AcessoAoGrupo(grupoId, donoUsuarioId));
}
