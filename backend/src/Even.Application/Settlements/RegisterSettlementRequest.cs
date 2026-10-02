namespace Even.Application.Settlements;

/// <summary>
/// Body of <c>POST /groups/{id}/settlements</c>: records that
/// <see cref="FromParticipantId"/> paid <see cref="AmountCents"/> to
/// <see cref="ToParticipantId"/>, settling (part of) a debt suggested by
/// <c>GET /groups/{id}/settlement</c>. Same role as
/// <c>Even.Application.Groups.SyncedExpenseRequest</c>: an edge DTO, used directly as
/// the controller's <c>[FromBody]</c> — no second type mirrored just in
/// <c>Even.Api.Contracts</c>. Validation (positive amount, payer != payee) happens when building
/// the domain type (<see cref="SettlementMapper"/>), not here.
/// </summary>
public sealed record RegisterSettlementRequest(Guid FromParticipantId, Guid ToParticipantId, long AmountCents);
