package com.rateio.domain.engine

import com.rateio.domain.model.Expense
import com.rateio.domain.model.Money
import com.rateio.domain.model.Settlement
import com.rateio.domain.model.SettlementSuggestion

/**
 * Contrato do motor de simplificação de dívidas (ver `algorithm-spec.md`). É o núcleo técnico do
 * projeto (constitution.md, princípio 4) — exposto como interface para que quem consome (a tela
 * "Quitar dívidas", T34) dependa de uma abstração, não da implementação concreta. Isso facilita o
 * teste isolado e permite trocar a estratégia de settlement no futuro sem quebrar quem chama.
 *
 * Espelha `IMotorDeSimplificacaoDeDividas` (`backend/src/Rateio.Domain/IMotorDeSimplificacaoDeDividas.cs`)
 * — mesmo contrato de duas funções, mesma regra de sinal, mesmo algoritmo guloso. As duas
 * implementações (esta e a C#) precisam produzir exatamente o mesmo resultado para a mesma
 * entrada; ver `algorithm-spec.md` para a especificação linguagem-agnóstica que ambas traduzem
 * linha a linha.
 */
interface DebtSimplificationEngine {

    /**
     * Recalcula do zero o saldo líquido de cada participante a partir do histórico completo de
     * despesas e quitações vigentes. Saldo positivo = a receber; negativo = a pagar; zero =
     * quitado. A soma de todos os saldos retornados é sempre zero — dinheiro não é criado nem
     * destruído pelo motor.
     */
    fun computeBalances(expenses: List<Expense>, settlements: List<Settlement>): Map<String, Money>

    /**
     * Dado o saldo líquido de cada participante, produz a lista mínima-o-suficiente de
     * transações (algoritmo guloso: maior devedor paga maior credor, reselecionados a cada
     * iteração) que zera todos os saldos.
     */
    fun computeSettlement(balances: Map<String, Money>): List<SettlementSuggestion>
}
