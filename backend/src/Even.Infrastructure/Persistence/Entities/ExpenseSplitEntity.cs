namespace Even.Infrastructure.Persistence.Entities;

/// <summary>
/// Persistence mapping of a participant's share of an expense. There's no domain type
/// equivalent to <see cref="Type"/> — <c>Even.Domain.ExpenseSplit</c> deliberately expresses the
/// split type via a concrete subtype (<c>Equal</c>/<c>Weighted</c>/<c>FixedAmount</c>) instead of a
/// bare enum, so there aren't two diverging sources of truth in the domain (see that type's XML
/// comment). Here it's different: this is just the discriminator column a relational table needs
/// to know which subtype to reconstruct on read — internal to Infrastructure, never leaks to the
/// domain.
/// </summary>
public class ExpenseSplitEntity
{
    public Guid Id { get; set; }

    public Guid ExpenseId { get; set; }

    public Guid ParticipantId { get; set; }

    public SplitTypeEntity Type { get; set; }

    /// <summary>Only filled in when <see cref="Type"/> is <see cref="SplitTypeEntity.Weighted"/>.</summary>
    public long? Weight { get; set; }

    /// <summary>Only filled in when <see cref="Type"/> is <see cref="SplitTypeEntity.FixedAmount"/>.</summary>
    public long? AmountCents { get; set; }

    public ExpenseEntity? Expense { get; set; }
}

/// <summary>Persistence discriminator for <see cref="ExpenseSplitEntity.Type"/> — see the class
/// comment for why this doesn't exist on the domain side.</summary>
public enum SplitTypeEntity
{
    Equal,
    Weighted,
    FixedAmount,
}
