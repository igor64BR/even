namespace Tally.Application.Settlements;

/// <summary>
/// Body of <c>POST /groups/{id}/settlements</c> (T35.1): records that
/// <see cref="FromParticipantId"/> paid <see cref="AmountCents"/> to
/// <see cref="ToParticipantId"/>, settling (part of) a debt suggested by
/// <c>GET /groups/{id}/settlement</c> (T32). Same role as
/// <c>Tally.Application.Groups.SyncedExpenseRequest</c> (T18/T23): an edge DTO, used directly as
/// the controller's <c>[FromBody]</c> — no second type mirrored just in
/// <c>Tally.Api.Contracts</c>. Validation (positive amount, payer != payee) happens when building
/// the domain type (<see cref="SettlementMapper"/>), not here.
/// </summary>
public sealed record RegisterSettlementRequest(Guid FromParticipantId, Guid ToParticipantId, long AmountCents);
