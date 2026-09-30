namespace Tally.Api.Contracts;

/// <summary>Success response of <c>POST /groups/{id}/settlements</c>.</summary>
public sealed record RegisterSettlementResponse(Guid SettlementId);
