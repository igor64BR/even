namespace Tally.Domain;

/// <summary>
/// Monetary amount used inside the simplification engine: a wrapped integer number of cents
/// (<see cref="long"/>), never raw. See "Money: representation and rounding" in
/// algorithm-spec.md — inside the algorithm there is no <c>float</c>/<c>double</c>/<c>decimal</c>;
/// those representations only exist at the parsing/formatting edge, outside this engine's scope.
/// </summary>
public readonly record struct Money : IComparable<Money>
{
    public long Cents { get; }

    private Money(long cents)
    {
        Cents = cents;
    }

    public static Money Zero { get; } = new(0);

    public static Money FromCents(long cents) => new(cents);

    public bool IsPositive => Cents > 0;

    public bool IsNegative => Cents < 0;

    public bool IsZero => Cents == 0;

    public static Money operator +(Money left, Money right) =>
        new(left.Cents + right.Cents);

    public static Money operator -(Money left, Money right) =>
        new(left.Cents - right.Cents);

    public static Money operator -(Money amount) => new(-amount.Cents);

    public int CompareTo(Money other) => Cents.CompareTo(other.Cents);

    public override string ToString() => Cents.ToString();
}
