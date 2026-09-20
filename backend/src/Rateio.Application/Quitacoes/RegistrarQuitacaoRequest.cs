namespace Rateio.Application.Quitacoes;

/// <summary>
/// Corpo de <c>POST /groups/{id}/settlements</c> (T35.1): registra que <see cref="DeParticipanteId"/>
/// pagou <see cref="ValorCentavos"/> a <see cref="ParaParticipanteId"/>, quitando (parte de) uma
/// dívida sugerida por <c>GET /groups/{id}/settlement</c> (T32). Mesmo papel de
/// <c>Rateio.Application.Grupos.DespesaSincronizadaRequest</c> (T18/T23): DTO de borda, usado
/// diretamente como <c>[FromBody]</c> do controller — sem um segundo tipo espelhado só em
/// <c>Rateio.Api.Contracts</c>. A validação (valor positivo, pagador ≠ recebedor) acontece ao
/// construir o tipo de domínio (<see cref="MapeadorDeQuitacao"/>), não aqui.
/// </summary>
public sealed record RegistrarQuitacaoRequest(Guid DeParticipanteId, Guid ParaParticipanteId, long ValorCentavos);
