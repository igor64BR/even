namespace Rateio.Api.Contracts;

/// <summary>Success response of <c>POST /groups/{id}/settlements</c> (T35.1).</summary>
public sealed record RegisterSettlementResponse(Guid SettlementId);
