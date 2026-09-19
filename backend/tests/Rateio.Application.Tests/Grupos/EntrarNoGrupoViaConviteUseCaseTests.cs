using Moq;
using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Application.Tests.Grupos;

/// <summary>
/// Cobre T21.2 com <see cref="ICodigoConviteRepository"/>/<see cref="IGrupoRepository"/> mockados:
/// nenhum teste aqui toca EF/Postgres. Entregável da task: "entrar com código válido adiciona o
/// participante" e "código inválido/inexistente recebe 404" — os dois primeiros testes; os demais
/// cobrem expiração (mesma exceção 404, ver <see cref="CodigoConvite"/>) e o caso defensivo de
/// grupo não encontrado.
/// </summary>
public class EntrarNoGrupoViaConviteUseCaseTests
{
    private static readonly DateTimeOffset Agora = new(2026, 1, 10, 12, 0, 0, TimeSpan.Zero);
    private const string NomeDoUsuario = "Carla";

    private readonly Mock<ICodigoConviteRepository> _codigoConviteRepository = new();
    private readonly Mock<IGrupoRepository> _grupoRepository = new();
    private readonly RelogioFixo _relogio = new(Agora);

    private EntrarNoGrupoViaConviteUseCase CriarUseCase() =>
        new(_codigoConviteRepository.Object, _grupoRepository.Object, _relogio);

    [Fact]
    public async Task ExecutarAsync_CodigoValido_AdicionaParticipanteAutenticadoERetornaOGrupo()
    {
        var grupoId = Guid.NewGuid();
        var anaId = Guid.NewGuid();
        ConfigurarCodigoValido("ABC12345", grupoId);
        ConfigurarGrupoParaEntrada(grupoId, ExistenteAutenticado(anaId, "Ana"));

        Participante? participanteAdicionado = null;
        var grupoIdRecebido = Guid.Empty;
        _grupoRepository
            .Setup(r => r.AdicionarParticipanteAsync(It.IsAny<Guid>(), It.IsAny<Participante>(), It.IsAny<CancellationToken>()))
            .Callback<Guid, Participante, CancellationToken>((grupo, participante, _) =>
            {
                grupoIdRecebido = grupo;
                participanteAdicionado = participante;
            })
            .Returns(Task.CompletedTask);

        var useCase = CriarUseCase();

        var resultado = await useCase.ExecutarAsync("ABC12345", NomeDoUsuario);

        Assert.Equal(grupoId, resultado);
        Assert.Equal(grupoId, grupoIdRecebido);
        Assert.NotNull(participanteAdicionado);
        Assert.Equal(NomeDoUsuario, participanteAdicionado!.Nome.Valor);
        Assert.False(participanteAdicionado.EhConvidado, "T21.2: quem entra via convite é participante autenticado, não convidado.");
        _grupoRepository.Verify(
            r => r.AdicionarParticipanteAsync(grupoId, It.IsAny<Participante>(), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ExecutarAsync_CodigoInexistente_LancaCodigoConviteInvalidoSemAdicionarParticipante()
    {
        _codigoConviteRepository
            .Setup(r => r.ObterPorCodigoAsync("NAOEXISTE", It.IsAny<CancellationToken>()))
            .ReturnsAsync((CodigoConvite?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<CodigoConviteInvalidoException>(
            () => useCase.ExecutarAsync("NAOEXISTE", NomeDoUsuario));

        Assert.Equal("NAOEXISTE", excecao.Codigo);
        _grupoRepository.Verify(
            r => r.AdicionarParticipanteAsync(It.IsAny<Guid>(), It.IsAny<Participante>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_CodigoExpirado_LancaCodigoConviteInvalidoSemAdicionarParticipante()
    {
        var grupoId = Guid.NewGuid();
        _codigoConviteRepository
            .Setup(r => r.ObterPorCodigoAsync("EXPIRADO", It.IsAny<CancellationToken>()))
            .ReturnsAsync(new CodigoConvite("EXPIRADO", grupoId, Agora.AddDays(-8), Agora.AddDays(-1)));

        var useCase = CriarUseCase();

        await Assert.ThrowsAsync<CodigoConviteInvalidoException>(
            () => useCase.ExecutarAsync("EXPIRADO", NomeDoUsuario));

        _grupoRepository.Verify(
            r => r.AdicionarParticipanteAsync(It.IsAny<Guid>(), It.IsAny<Participante>(), It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task ExecutarAsync_GrupoDoConviteNaoExisteMais_LancaGrupoNaoEncontrado()
    {
        var grupoId = Guid.NewGuid();
        ConfigurarCodigoValido("ABC12345", grupoId);
        _grupoRepository
            .Setup(r => r.ObterParaEntradaAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((GrupoParaEntrada?)null);

        var useCase = CriarUseCase();

        var excecao = await Assert.ThrowsAsync<GrupoNaoEncontradoException>(
            () => useCase.ExecutarAsync("ABC12345", NomeDoUsuario));

        Assert.Equal(grupoId, excecao.GrupoId);
    }

    private void ConfigurarCodigoValido(string codigo, Guid grupoId) =>
        _codigoConviteRepository
            .Setup(r => r.ObterPorCodigoAsync(codigo, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new CodigoConvite(codigo, grupoId, Agora.AddDays(-1), Agora.AddDays(6)));

    private void ConfigurarGrupoParaEntrada(Guid grupoId, params Participante[] participantesExistentes) =>
        _grupoRepository
            .Setup(r => r.ObterParaEntradaAsync(grupoId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GrupoParaEntrada(
                NomeGrupo.Criar("Viagem"),
                CategoriaGrupo.Viagem,
                participantesExistentes));

    private static Participante ExistenteAutenticado(Guid id, string nome) =>
        Participante.Autenticado(new ParticipanteId(id), NomeParticipante.Criar(nome));
}
