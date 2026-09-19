namespace Rateio.Domain;

/// <summary>
/// A parte de um participante numa <see cref="Despesa"/>. O tipo concreto já carrega a regra de
/// divisão aplicável (Igual, Percentual/peso ou ValorFixo) — não existe um enum
/// <c>TipoDivisao</c> separado guardando essa informação de novo, porque isso deixaria dois
/// lugares que poderiam divergir (o enum dizendo uma coisa, os itens de
/// <see cref="Despesa.Participacoes"/> sendo de outro tipo). Todas as participações de uma
/// mesma despesa devem ser do mesmo subtipo concreto; validar essa consistência é
/// responsabilidade da borda de entrada (DTO/formulário), não deste tipo nem do motor.
/// </summary>
public abstract record ParticipacaoDespesa(ParticipanteId ParticipanteId)
{
    /// <summary>Divisão em partes iguais entre todos os participantes (RF17).</summary>
    public sealed record PorIgual(ParticipanteId ParticipanteId) : ParticipacaoDespesa(ParticipanteId);

    /// <summary>Divisão proporcional a um peso/percentual por participante (RF18).</summary>
    public sealed record PorPeso(ParticipanteId ParticipanteId, long Peso) : ParticipacaoDespesa(ParticipanteId);

    /// <summary>Divisão por valor fixo definido por participante (RF19).</summary>
    public sealed record PorValorFixo(ParticipanteId ParticipanteId, Dinheiro Valor) : ParticipacaoDespesa(ParticipanteId);
}
