namespace Even.Domain;

/// <summary>
/// Expense logged in a group — minimal representation to feed the simplification engine
/// (<see cref="IDebtSimplificationEngine"/>). Not the full persistence model; that's
/// Even.Infrastructure/EF Core's scope, from another task.
/// </summary>
public sealed record Expense(
    Guid Id,
    Money TotalAmount,
    ParticipantId PayerId,
    IReadOnlyList<ExpenseSplit> Splits);
