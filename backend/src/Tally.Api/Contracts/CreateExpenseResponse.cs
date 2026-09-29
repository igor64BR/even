namespace Tally.Api.Contracts;

/// <summary>Success response of <c>POST /groups/{id}/expenses</c> (T23.1).</summary>
public sealed record CreateExpenseResponse(Guid ExpenseId);
