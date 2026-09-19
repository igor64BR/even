using Rateio.Domain;

namespace Rateio.Domain.Tests;

/// <summary>
/// Cobre a distinção convidado vs autenticado (RF06/RF07 — spec.md) e a validação de
/// <see cref="NomeParticipante"/>/<see cref="NomeGrupo"/> (não podem ser vazios/só espaço).
/// </summary>
public class ParticipanteTests
{
    [Fact]
    public void Convidado_CriaParticipanteSemConta()
    {
        var id = ParticipanteId.NovoId();

        var participante = Participante.Convidado(id, NomeParticipante.Criar("Ana"));

        Assert.True(participante.EhConvidado);
        Assert.Equal(id, participante.Id);
        Assert.Equal("Ana", participante.Nome.Valor);
    }

    [Fact]
    public void Autenticado_CriaParticipanteVinculadoAConta()
    {
        var participante = Participante.Autenticado(ParticipanteId.NovoId(), NomeParticipante.Criar("Bruno"));

        Assert.False(participante.EhConvidado);
    }

    [Fact]
    public void Equals_ComparaPorId_IgnorandoOutrosCampos()
    {
        var id = ParticipanteId.NovoId();
        var comoConvidado = Participante.Convidado(id, NomeParticipante.Criar("Ana"));
        var comoAutenticado = Participante.Autenticado(id, NomeParticipante.Criar("Ana Autenticada"));

        Assert.Equal(comoConvidado, comoAutenticado);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void NomeParticipante_Criar_ComValorVazio_Falha(string valorInvalido)
    {
        Assert.Throws<ArgumentException>(() => NomeParticipante.Criar(valorInvalido));
    }

    [Fact]
    public void NomeParticipante_Criar_RemoveEspacosNasBordas()
    {
        var nome = NomeParticipante.Criar("  Ana  ");

        Assert.Equal("Ana", nome.Valor);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void NomeGrupo_Criar_ComValorVazio_Falha(string valorInvalido)
    {
        Assert.Throws<ArgumentException>(() => NomeGrupo.Criar(valorInvalido));
    }
}
