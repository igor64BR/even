namespace Rateio.Api.Contracts;

/// <summary>
/// Resposta de sucesso de <c>POST /groups/{id}/invite-code</c> (T21.1). <see cref="ExpiraEm"/>
/// deixa explícito pro app quando o código deixa de ser válido — ver <c>Rateio.Application.Grupos.CodigoConvite</c>
/// para a política de expiração/reuso.
/// </summary>
public sealed record GerarCodigoConviteResponse(string Codigo, DateTimeOffset ExpiraEm);
