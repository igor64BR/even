using Rateio.Domain;

namespace Rateio.Domain.Tests;

/// <summary>
/// Um teste por caso da tabela "Casos de teste" em
/// specs/001-mvp-expense-splitting/algorithm-spec.md, nomeado com o id estável de cada caso
/// (ex. <c>case-01-simples</c> -&gt; <see cref="Case01Simples_UmaDividaEntreDuasPessoas"/>).
/// Os cinco casos <c>case-*</c> são o piso mínimo exigido pela spec e pela T31; os testes
/// extras no fim do arquivo (divisão por peso e por valor fixo) cobrem os outros dois modos de
/// divisão descritos na spec, que a T31 também exercita mas que não têm um caso estável próprio.
/// </summary>
public class MotorDeSimplificacaoDeDividasTests
{
    private readonly IMotorDeSimplificacaoDeDividas _motor = new MotorDeSimplificacaoDeDividas();

    [Fact]
    public void Case01Simples_UmaDividaEntreDuasPessoas()
    {
        var a = ParticipanteComOrdinal(1);
        var b = ParticipanteComOrdinal(2);
        var saldos = new Dictionary<ParticipanteId, Dinheiro>
        {
            [a] = Dinheiro.EmCentavos(-1000),
            [b] = Dinheiro.EmCentavos(1000),
        };

        var transacoes = _motor.ComputeSettlement(saldos);

        Assert.Equal(new[] { new Transacao(a, b, Dinheiro.EmCentavos(1000)) }, transacoes);
    }

    [Fact]
    public void Case02Ciclo_DividasParAParSeCancelamNoSaldoLiquido()
    {
        var saldos = new Dictionary<ParticipanteId, Dinheiro>
        {
            [ParticipanteComOrdinal(1)] = Dinheiro.Zero,
            [ParticipanteComOrdinal(2)] = Dinheiro.Zero,
            [ParticipanteComOrdinal(3)] = Dinheiro.Zero,
        };

        var transacoes = _motor.ComputeSettlement(saldos);

        Assert.Empty(transacoes);
    }

    [Fact]
    public void Case03Zerado_GrupoJaQuitadoNaoGeraTransacao()
    {
        var saldos = new Dictionary<ParticipanteId, Dinheiro>
        {
            [ParticipanteComOrdinal(1)] = Dinheiro.Zero,
            [ParticipanteComOrdinal(2)] = Dinheiro.Zero,
            [ParticipanteComOrdinal(3)] = Dinheiro.Zero,
        };

        var transacoes = _motor.ComputeSettlement(saldos);

        Assert.Empty(transacoes);
    }

    [Fact]
    public void Case04CadeiaLonga_GulosoResolveCincoParticipantesEmTresTransacoes()
    {
        var a = ParticipanteComOrdinal(1);
        var b = ParticipanteComOrdinal(2);
        var c = ParticipanteComOrdinal(3);
        var d = ParticipanteComOrdinal(4);
        var e = ParticipanteComOrdinal(5);
        var saldos = new Dictionary<ParticipanteId, Dinheiro>
        {
            [a] = Dinheiro.EmCentavos(-4000),
            [b] = Dinheiro.EmCentavos(-3000),
            [c] = Dinheiro.EmCentavos(1000),
            [d] = Dinheiro.EmCentavos(2000),
            [e] = Dinheiro.EmCentavos(4000),
        };

        var transacoes = _motor.ComputeSettlement(saldos);

        var esperado = new[]
        {
            new Transacao(a, e, Dinheiro.EmCentavos(4000)),
            new Transacao(b, d, Dinheiro.EmCentavos(2000)),
            new Transacao(b, c, Dinheiro.EmCentavos(1000)),
        };
        Assert.Equal(esperado, transacoes);
    }

    [Fact]
    public void Case05Arredondamento_FechamentoPorMaioresRestosNaoPerdeCentavo()
    {
        var p1 = ParticipanteComOrdinal(1);
        var p2 = ParticipanteComOrdinal(2);
        var p3 = ParticipanteComOrdinal(3);
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: p1,
            Participacoes: new ParticipacaoDespesa[]
            {
                new ParticipacaoDespesa.PorIgual(p1),
                new ParticipacaoDespesa.PorIgual(p2),
                new ParticipacaoDespesa.PorIgual(p3),
            });

        var saldos = _motor.ComputeBalances(new[] { despesa }, quitacoes: Array.Empty<Quitacao>());

        Assert.Equal(Dinheiro.EmCentavos(666), saldos[p1]);
        Assert.Equal(Dinheiro.EmCentavos(-333), saldos[p2]);
        Assert.Equal(Dinheiro.EmCentavos(-333), saldos[p3]);
        Assert.Equal(0, SomaCentavos(saldos));

        var transacoes = _motor.ComputeSettlement(saldos);

        var esperado = new[]
        {
            new Transacao(p2, p1, Dinheiro.EmCentavos(333)),
            new Transacao(p3, p1, Dinheiro.EmCentavos(333)),
        };
        Assert.Equal(esperado, transacoes);
    }

    [Fact]
    public void ComputeBalances_SomaDosSaldosEhSempreZero_MesmoComDespesaEQuitacao()
    {
        var p1 = ParticipanteComOrdinal(1);
        var p2 = ParticipanteComOrdinal(2);
        var p3 = ParticipanteComOrdinal(3);
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: p1,
            Participacoes: new ParticipacaoDespesa[]
            {
                new ParticipacaoDespesa.PorIgual(p1),
                new ParticipacaoDespesa.PorIgual(p2),
                new ParticipacaoDespesa.PorIgual(p3),
            });
        var quitacao = new Quitacao(Guid.NewGuid(), PagadorId: p2, RecebedorId: p1, Dinheiro.EmCentavos(100));

        var saldos = _motor.ComputeBalances(new[] { despesa }, new[] { quitacao });

        Assert.Equal(0, SomaCentavos(saldos));
    }

    [Fact]
    public void ComputeBalances_DivisaoPorPeso_UsaMetodoDosMaioresRestos()
    {
        var x = ParticipanteComOrdinal(1);
        var y = ParticipanteComOrdinal(2);
        var z = ParticipanteComOrdinal(3);
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(100),
            PagadorId: x,
            Participacoes: new ParticipacaoDespesa[]
            {
                new ParticipacaoDespesa.PorPeso(x, Peso: 1),
                new ParticipacaoDespesa.PorPeso(y, Peso: 1),
                new ParticipacaoDespesa.PorPeso(z, Peso: 1),
            });

        var saldos = _motor.ComputeBalances(new[] { despesa }, quitacoes: Array.Empty<Quitacao>());

        // 100 / 3 = base 33, resto 1 -> x (primeiro em ordem de id) recebe o centavo extra: 34/33/33.
        Assert.Equal(Dinheiro.EmCentavos(66), saldos[x]);
        Assert.Equal(Dinheiro.EmCentavos(-33), saldos[y]);
        Assert.Equal(Dinheiro.EmCentavos(-33), saldos[z]);
        Assert.Equal(0, SomaCentavos(saldos));
    }

    [Fact]
    public void ComputeBalances_DivisaoPorValorFixo_UsaValorDeCadaParticipanteSemArredondar()
    {
        var x = ParticipanteComOrdinal(1);
        var y = ParticipanteComOrdinal(2);
        var despesa = new Despesa(
            Guid.NewGuid(),
            Dinheiro.EmCentavos(1000),
            PagadorId: x,
            Participacoes: new ParticipacaoDespesa[]
            {
                new ParticipacaoDespesa.PorValorFixo(x, Dinheiro.EmCentavos(400)),
                new ParticipacaoDespesa.PorValorFixo(y, Dinheiro.EmCentavos(600)),
            });

        var saldos = _motor.ComputeBalances(new[] { despesa }, quitacoes: Array.Empty<Quitacao>());

        Assert.Equal(Dinheiro.EmCentavos(600), saldos[x]);
        Assert.Equal(Dinheiro.EmCentavos(-600), saldos[y]);
        Assert.Equal(0, SomaCentavos(saldos));
    }

    private static ParticipanteId ParticipanteComOrdinal(int ordinal) =>
        new(Guid.Parse($"00000000-0000-0000-0000-{ordinal:D12}"));

    private static long SomaCentavos(IReadOnlyDictionary<ParticipanteId, Dinheiro> saldos) =>
        saldos.Values.Sum(saldo => saldo.Centavos);
}
