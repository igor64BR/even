namespace Rateio.Domain;

/// <summary>
/// Despesa lançada num grupo — representação mínima para alimentar o motor de simplificação
/// (<see cref="IMotorDeSimplificacaoDeDividas"/>). Não é o modelo de persistência completo;
/// isso é escopo de Rateio.Infrastructure/EF Core, de outra task.
/// </summary>
public sealed record Despesa(
    Guid Id,
    Dinheiro ValorTotal,
    ParticipanteId PagadorId,
    IReadOnlyList<ParticipacaoDespesa> Participacoes);
