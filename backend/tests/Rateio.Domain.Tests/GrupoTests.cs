using Rateio.Domain;

namespace Rateio.Domain.Tests;

/// <summary>
/// Cobre a invariante central de <see cref="Grupo"/> (T15.1 — pelo menos 1 participante, sempre)
/// e o comportamento dos métodos nomeados de mutação (<see cref="Grupo.AdicionarParticipante"/>,
/// <see cref="Grupo.RemoverParticipante"/>, <see cref="Grupo.MarcarComoSincronizado"/>).
/// </summary>
public class GrupoTests
{
    [Fact]
    public void Criar_SemNenhumParticipante_Falha()
    {
        var excecao = Assert.Throws<InvalidOperationException>(
            () => Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, []));

        Assert.Contains("pelo menos 1 participante", excecao.Message);
    }

    [Fact]
    public void Criar_ComUmParticipante_Funciona()
    {
        var voce = ParticipanteConvidado("Você");

        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [voce]);

        Assert.Single(grupo.Participantes);
        Assert.Equal(voce, grupo.Participantes[0]);
        Assert.False(grupo.Sincronizado);
    }

    [Fact]
    public void AdicionarParticipante_ComIdNovo_IncluiNoGrupo()
    {
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [ParticipanteConvidado("Você")]);
        var amigo = ParticipanteConvidado("Amigo");

        grupo.AdicionarParticipante(amigo);

        Assert.Equal(2, grupo.Participantes.Count);
        Assert.Contains(amigo, grupo.Participantes);
    }

    [Fact]
    public void AdicionarParticipante_ComIdJaExistente_Falha()
    {
        var voce = ParticipanteConvidado("Você");
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [voce]);
        var duplicado = Participante.Convidado(voce.Id, NomeParticipante.Criar("Outro Nome"));

        Assert.Throws<InvalidOperationException>(() => grupo.AdicionarParticipante(duplicado));
    }

    [Fact]
    public void RemoverParticipante_QuandoSobraPeloMenosUm_Funciona()
    {
        var voce = ParticipanteConvidado("Você");
        var amigo = ParticipanteConvidado("Amigo");
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [voce, amigo]);

        grupo.RemoverParticipante(amigo.Id);

        Assert.Single(grupo.Participantes);
        Assert.Equal(voce, grupo.Participantes[0]);
    }

    [Fact]
    public void RemoverParticipante_QuandoRestaApenasUm_Falha()
    {
        var voce = ParticipanteConvidado("Você");
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [voce]);

        var excecao = Assert.Throws<InvalidOperationException>(() => grupo.RemoverParticipante(voce.Id));

        Assert.Contains("pelo menos 1 participante", excecao.Message);
        Assert.Single(grupo.Participantes);
    }

    [Fact]
    public void RemoverParticipante_QueNaoEstaNoGrupo_Falha()
    {
        var voce = ParticipanteConvidado("Você");
        var amigo = ParticipanteConvidado("Amigo");
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [voce, amigo]);

        Assert.Throws<InvalidOperationException>(() => grupo.RemoverParticipante(ParticipanteId.NovoId()));
    }

    [Fact]
    public void MarcarComoSincronizado_MudaFlagParaTrue()
    {
        var grupo = Grupo.Criar(NomeGrupo.Criar("Viagem"), CategoriaGrupo.Viagem, [ParticipanteConvidado("Você")]);

        grupo.MarcarComoSincronizado();

        Assert.True(grupo.Sincronizado);
    }

    private static Participante ParticipanteConvidado(string nome) =>
        Participante.Convidado(ParticipanteId.NovoId(), NomeParticipante.Criar(nome));
}
