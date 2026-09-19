namespace Rateio.Domain;

/// <summary>
/// Registro de que <see cref="PagadorId"/> pagou <see cref="Valor"/> a <see cref="RecebedorId"/>
/// para quitar (parte de) uma dívida existente (RF31/RF33).
/// </summary>
public sealed record Quitacao(
    Guid Id,
    ParticipanteId PagadorId,
    ParticipanteId RecebedorId,
    Dinheiro Valor);
