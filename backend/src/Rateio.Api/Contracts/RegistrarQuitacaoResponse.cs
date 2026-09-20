namespace Rateio.Api.Contracts;

/// <summary>Resposta de sucesso de <c>POST /groups/{id}/settlements</c> (T35.1).</summary>
public sealed record RegistrarQuitacaoResponse(Guid QuitacaoId);
