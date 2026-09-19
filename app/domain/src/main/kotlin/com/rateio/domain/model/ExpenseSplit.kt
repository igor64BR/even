package com.rateio.domain.model

/**
 * A parte de um [participantId] numa [Expense]. Espelha `ParticipacaoDespesa`
 * (`backend/src/Rateio.Domain/ParticipacaoDespesa.cs`): o subtipo concreto já carrega a regra de
 * divisão aplicável (Igual/Peso/ValorFixo, RF17-RF19) — não existe um enum `SplitType` solto do
 * lado, porque isso deixaria dois lugares que poderiam divergir (o enum dizendo uma coisa, o item
 * sendo de outro subtipo). `sealed class` com subtipos aninhados é o equivalente natural Kotlin da
 * hierarquia `abstract record` + `sealed record`s aninhados que `ParticipacaoDespesa.cs` usa em C#.
 *
 * Todas as participações de uma mesma despesa devem ser do mesmo subtipo concreto; validar essa
 * consistência é responsabilidade da borda de entrada (formulário/DTO), não deste tipo nem do
 * motor de simplificação — mesma decisão documentada em `ParticipacaoDespesa.cs` e em
 * `algorithm-spec.md`.
 */
sealed class ExpenseSplit {
    abstract val participantId: String

    /** Divisão em partes iguais entre todos os participantes (RF17). */
    data class Equal(override val participantId: String) : ExpenseSplit()

    /** Divisão proporcional a um peso/percentual por participante (RF18). */
    data class Weight(override val participantId: String, val weight: Long) : ExpenseSplit()

    /** Divisão por valor fixo definido por participante (RF19). */
    data class FixedAmount(override val participantId: String, val amount: Money) : ExpenseSplit()
}
