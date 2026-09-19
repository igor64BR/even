namespace Rateio.Domain;

/// <summary>
/// Uma transação sugerida pelo motor de simplificação: "<see cref="De"/> deve pagar
/// <see cref="Valor"/> para <see cref="Para"/>".
/// </summary>
public sealed record Transacao(ParticipanteId De, ParticipanteId Para, Dinheiro Valor);
