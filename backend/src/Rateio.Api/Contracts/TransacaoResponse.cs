namespace Rateio.Api.Contracts;

/// <summary>
/// Uma transação sugerida por <c>GET /groups/{id}/settlement</c> (T32.1): "<see cref="De"/> deve
/// pagar <see cref="ValorCentavos"/> para <see cref="Para"/>" — serialização HTTP de
/// <c>Rateio.Domain.Transacao</c>, trocando <c>ParticipanteId</c>/<c>Dinheiro</c> (tipos wrapping
/// internos do domínio) pelos primitivos (<see cref="Guid"/>/<see cref="long"/>) que o contrato
/// público da API expõe.
/// </summary>
public sealed record TransacaoResponse(Guid De, Guid Para, long ValorCentavos);
