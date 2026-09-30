namespace Tally.Api.Contracts;

/// <summary>Success response of <c>POST /groups/{id}/expenses</c>.</summary>
public sealed record CreateExpenseResponse(Guid ExpenseId);
