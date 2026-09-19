namespace Rateio.Api.Contracts;

/// <summary>Resposta de sucesso de <c>POST /groups/{id}/expenses</c> (T23.1).</summary>
public sealed record CriarDespesaResponse(Guid DespesaId);
