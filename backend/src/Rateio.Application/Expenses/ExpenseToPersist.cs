using Rateio.Domain;

namespace Rateio.Application.Expenses;

/// <summary>
/// An expense ready to persist: the domain record (used by the simplification engine) plus the
/// fields that only exist at the persistence edge (description, entry date). Extracted from
/// <c>Rateio.Application.Groups.ExpenseToPersist</c> (T18.1) to be shared with T23
/// (<c>POST /groups/{id}/expenses</c>) — both the sync bulk load (<c>GroupToSync.Expenses</c>) and
/// the single-expense endpoint (<see cref="IExpenseRepository.AddAsync"/>) persist exactly this
/// same shape.
/// </summary>
public sealed record ExpenseToPersist(Expense Expense, string Description, DateOnly Date);
