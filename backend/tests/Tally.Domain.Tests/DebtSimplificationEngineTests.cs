using Tally.Domain;

namespace Tally.Domain.Tests;

/// <summary>
/// One test per case in the "Casos de teste" table in
/// specs/001-mvp-expense-splitting/algorithm-spec.md, named after each case's stable id
/// (e.g. <c>case-01-simples</c> -&gt; <see cref="Case01Simple_OneDebtBetweenTwoPeople"/>).
/// The five <c>case-*</c> cases are the minimum floor required by the spec and by T31; the extra
/// tests at the end of the file (weighted split and fixed-amount split) cover the other two split
/// modes described in the spec, which T31 also exercises but that don't have their own stable
/// case id.
/// </summary>
public class DebtSimplificationEngineTests
{
    private readonly IDebtSimplificationEngine _engine = new DebtSimplificationEngine();

    [Fact]
    public void Case01Simple_OneDebtBetweenTwoPeople()
    {
        var a = ParticipantWithOrdinal(1);
        var b = ParticipantWithOrdinal(2);
        var balances = new Dictionary<ParticipantId, Money>
        {
            [a] = Money.FromCents(-1000),
            [b] = Money.FromCents(1000),
        };

        var transactions = _engine.ComputeSettlement(balances);

        Assert.Equal(new[] { new Transaction(a, b, Money.FromCents(1000)) }, transactions);
    }

    [Fact]
    public void Case02Cycle_PairwiseDebtsCancelOutInNetBalance()
    {
        var balances = new Dictionary<ParticipantId, Money>
        {
            [ParticipantWithOrdinal(1)] = Money.Zero,
            [ParticipantWithOrdinal(2)] = Money.Zero,
            [ParticipantWithOrdinal(3)] = Money.Zero,
        };

        var transactions = _engine.ComputeSettlement(balances);

        Assert.Empty(transactions);
    }

    [Fact]
    public void Case03Zeroed_AlreadySettledGroupGeneratesNoTransaction()
    {
        var balances = new Dictionary<ParticipantId, Money>
        {
            [ParticipantWithOrdinal(1)] = Money.Zero,
            [ParticipantWithOrdinal(2)] = Money.Zero,
            [ParticipantWithOrdinal(3)] = Money.Zero,
        };

        var transactions = _engine.ComputeSettlement(balances);

        Assert.Empty(transactions);
    }

    [Fact]
    public void Case04LongChain_GreedyResolvesFiveParticipantsInThreeTransactions()
    {
        var a = ParticipantWithOrdinal(1);
        var b = ParticipantWithOrdinal(2);
        var c = ParticipantWithOrdinal(3);
        var d = ParticipantWithOrdinal(4);
        var e = ParticipantWithOrdinal(5);
        var balances = new Dictionary<ParticipantId, Money>
        {
            [a] = Money.FromCents(-4000),
            [b] = Money.FromCents(-3000),
            [c] = Money.FromCents(1000),
            [d] = Money.FromCents(2000),
            [e] = Money.FromCents(4000),
        };

        var transactions = _engine.ComputeSettlement(balances);

        var expected = new[]
        {
            new Transaction(a, e, Money.FromCents(4000)),
            new Transaction(b, d, Money.FromCents(2000)),
            new Transaction(b, c, Money.FromCents(1000)),
        };
        Assert.Equal(expected, transactions);
    }

    [Fact]
    public void Case05Rounding_LargestRemainderClosingDoesNotLoseACent()
    {
        var p1 = ParticipantWithOrdinal(1);
        var p2 = ParticipantWithOrdinal(2);
        var p3 = ParticipantWithOrdinal(3);
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: p1,
            Splits: new ExpenseSplit[]
            {
                new ExpenseSplit.Equal(p1),
                new ExpenseSplit.Equal(p2),
                new ExpenseSplit.Equal(p3),
            });

        var balances = _engine.ComputeBalances(new[] { expense }, settlements: Array.Empty<Settlement>());

        Assert.Equal(Money.FromCents(666), balances[p1]);
        Assert.Equal(Money.FromCents(-333), balances[p2]);
        Assert.Equal(Money.FromCents(-333), balances[p3]);
        Assert.Equal(0, SumCents(balances));

        var transactions = _engine.ComputeSettlement(balances);

        var expected = new[]
        {
            new Transaction(p2, p1, Money.FromCents(333)),
            new Transaction(p3, p1, Money.FromCents(333)),
        };
        Assert.Equal(expected, transactions);
    }

    [Fact]
    public void ComputeBalances_SumOfBalancesIsAlwaysZero_EvenWithExpenseAndSettlement()
    {
        var p1 = ParticipantWithOrdinal(1);
        var p2 = ParticipantWithOrdinal(2);
        var p3 = ParticipantWithOrdinal(3);
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: p1,
            Splits: new ExpenseSplit[]
            {
                new ExpenseSplit.Equal(p1),
                new ExpenseSplit.Equal(p2),
                new ExpenseSplit.Equal(p3),
            });
        var settlement = new Settlement(Guid.NewGuid(), PayerId: p2, PayeeId: p1, Money.FromCents(100));

        var balances = _engine.ComputeBalances(new[] { expense }, new[] { settlement });

        Assert.Equal(0, SumCents(balances));
    }

    [Fact]
    public void ComputeBalances_WeightedSplit_UsesLargestRemainderMethod()
    {
        var x = ParticipantWithOrdinal(1);
        var y = ParticipantWithOrdinal(2);
        var z = ParticipantWithOrdinal(3);
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(100),
            PayerId: x,
            Splits: new ExpenseSplit[]
            {
                new ExpenseSplit.Weighted(x, Weight: 1),
                new ExpenseSplit.Weighted(y, Weight: 1),
                new ExpenseSplit.Weighted(z, Weight: 1),
            });

        var balances = _engine.ComputeBalances(new[] { expense }, settlements: Array.Empty<Settlement>());

        // 100 / 3 = base 33, remainder 1 -> x (first in id order) gets the extra cent: 34/33/33.
        Assert.Equal(Money.FromCents(66), balances[x]);
        Assert.Equal(Money.FromCents(-33), balances[y]);
        Assert.Equal(Money.FromCents(-33), balances[z]);
        Assert.Equal(0, SumCents(balances));
    }

    [Fact]
    public void ComputeBalances_FixedAmountSplit_UsesEachParticipantsAmountWithoutRounding()
    {
        var x = ParticipantWithOrdinal(1);
        var y = ParticipantWithOrdinal(2);
        var expense = new Expense(
            Guid.NewGuid(),
            Money.FromCents(1000),
            PayerId: x,
            Splits: new ExpenseSplit[]
            {
                new ExpenseSplit.FixedAmount(x, Money.FromCents(400)),
                new ExpenseSplit.FixedAmount(y, Money.FromCents(600)),
            });

        var balances = _engine.ComputeBalances(new[] { expense }, settlements: Array.Empty<Settlement>());

        Assert.Equal(Money.FromCents(600), balances[x]);
        Assert.Equal(Money.FromCents(-600), balances[y]);
        Assert.Equal(0, SumCents(balances));
    }

    private static ParticipantId ParticipantWithOrdinal(int ordinal) =>
        new(Guid.Parse($"00000000-0000-0000-0000-{ordinal:D12}"));

    private static long SumCents(IReadOnlyDictionary<ParticipantId, Money> balances) =>
        balances.Values.Sum(balance => balance.Cents);
}
