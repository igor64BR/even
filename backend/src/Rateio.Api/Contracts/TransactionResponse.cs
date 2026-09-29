namespace Rateio.Api.Contracts;

/// <summary>
/// A transaction suggested by <c>GET /groups/{id}/settlement</c> (T32.1): "<see cref="From"/> should
/// pay <see cref="AmountCents"/> to <see cref="To"/>" — HTTP serialization of
/// <c>Rateio.Domain.Transaction</c>, swapping <c>ParticipantId</c>/<c>Money</c> (the domain's
/// internal wrapping types) for the primitives (<see cref="Guid"/>/<see cref="long"/>) the API's
/// public contract exposes.
/// </summary>
public sealed record TransactionResponse(Guid From, Guid To, long AmountCents);
