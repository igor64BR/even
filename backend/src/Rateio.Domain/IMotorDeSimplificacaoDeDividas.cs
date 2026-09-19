namespace Rateio.Domain;

/// <summary>
/// Contrato do motor de simplificação de dívidas (ver algorithm-spec.md). É o núcleo técnico do
/// projeto (constituição, princípio 4) — exposto como interface para que quem consome (camada
/// de aplicação/API) dependa de uma abstração, não da implementação concreta. Isso facilita o
/// teste isolado e permite trocar a estratégia de settlement no futuro sem quebrar quem chama.
/// </summary>
public interface IMotorDeSimplificacaoDeDividas
{
    /// <summary>
    /// Recalcula do zero o saldo líquido de cada participante a partir do histórico completo de
    /// despesas e quitações vigentes. Saldo positivo = a receber; negativo = a pagar; zero =
    /// quitado. A soma de todos os saldos retornados é sempre zero.
    /// </summary>
    IReadOnlyDictionary<ParticipanteId, Dinheiro> ComputeBalances(
        IReadOnlyList<Despesa> despesas,
        IReadOnlyList<Quitacao> quitacoes);

    /// <summary>
    /// Dado o saldo líquido de cada participante, produz a lista mínima-o-suficiente de
    /// transações (algoritmo guloso) que zera todos os saldos.
    /// </summary>
    IReadOnlyList<Transacao> ComputeSettlement(IReadOnlyDictionary<ParticipanteId, Dinheiro> saldos);
}
