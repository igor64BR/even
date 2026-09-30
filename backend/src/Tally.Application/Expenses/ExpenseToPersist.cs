using Tally.Domain;

namespace Tally.Application.Expenses;

/// <summary>
/// An expense ready to persist: the domain record (used by the simplification engine) plus the
/// fields that only exist at the persistence edge (description, entry date). Shared between
/// the sync bulk load (<c>GroupToSync.Expenses</c>) and
/// the single-expense endpoint (<see cref="IExpenseRepository.AddAsync"/>), which persist exactly
/// this same shape.
/// </summary>
public sealed record ExpenseToPersist(Expense Expense, string Description, DateOnly Date);
