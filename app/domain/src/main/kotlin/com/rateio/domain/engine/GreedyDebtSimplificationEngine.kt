package com.rateio.domain.engine

import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Money
import com.rateio.domain.model.Settlement
import com.rateio.domain.model.SettlementSuggestion

/**
 * Implementação de referência de [DebtSimplificationEngine]: tradução linha a linha do
 * pseudocódigo em `algorithm-spec.md`. Porta direta de `MotorDeSimplificacaoDeDividas`
 * (`backend/src/Rateio.Domain/MotorDeSimplificacaoDeDividas.cs`, T31) — mesma regra de sinal,
 * mesmo algoritmo guloso com reseleção do maior devedor/credor a cada iteração (não dois
 * ponteiros sobre listas ordenadas uma única vez — ver a ressalva na seção `computeSettlement` de
 * `algorithm-spec.md`), mesmo método dos maiores restos para fechar divisões que não dão exato.
 *
 * Convenção de nomes: os membros públicos do contrato ([computeBalances]/[computeSettlement])
 * usam os nomes em inglês da spec para bater exatamente com ela, espelhando a mesma escolha do
 * lado C#; métodos privados de apoio usam nomes em português (despesa, quitação, saldo),
 * consistente com o domínio do projeto.
 */
class GreedyDebtSimplificationEngine : DebtSimplificationEngine {

    override fun computeBalances(expenses: List<Expense>, settlements: List<Settlement>): Map<String, Money> {
        val saldos = mutableMapOf<String, Money>()

        expenses.forEach { despesa -> aplicarDespesa(saldos, despesa) }
        settlements.forEach { quitacao -> aplicarQuitacao(saldos, quitacao) }

        return saldos
    }

    override fun computeSettlement(balances: Map<String, Money>): List<SettlementSuggestion> {
        val devedores = participantesComSaldoNegativo(balances).toMutableList()
        val credores = participantesComSaldoPositivo(balances).toMutableList()
        val transacoes = mutableListOf<SettlementSuggestion>()

        while (devedores.isNotEmpty() && credores.isNotEmpty()) {
            transacoes += quitarMaiorDevedorComMaiorCredor(devedores, credores)
        }

        return transacoes
    }

    // --- computeBalances -----------------------------------------------------------------

    private fun aplicarDespesa(saldos: MutableMap<String, Money>, despesa: Expense) {
        val total = Money.ofCents(despesa.amountCents)
        creditar(saldos, despesa.paidByParticipantId, total)

        val partes = dividir(total, despesa.splits)
        partes.forEach { (participantId, parte) -> debitar(saldos, participantId, parte) }
    }

    private fun aplicarQuitacao(saldos: MutableMap<String, Money>, quitacao: Settlement) {
        creditar(saldos, quitacao.payerId, quitacao.amount)
        debitar(saldos, quitacao.receiverId, quitacao.amount)
    }

    private fun creditar(saldos: MutableMap<String, Money>, participantId: String, valor: Money) {
        saldos[participantId] = saldoAtual(saldos, participantId) + valor
    }

    private fun debitar(saldos: MutableMap<String, Money>, participantId: String, valor: Money) {
        saldos[participantId] = saldoAtual(saldos, participantId) - valor
    }

    private fun saldoAtual(saldos: Map<String, Money>, participantId: String): Money =
        saldos[participantId] ?: Money.ZERO

    // --- divisão da despesa (fechamento por maiores restos) ------------------------------

    private fun dividir(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        require(splits.isNotEmpty()) { "Despesa sem participantes na divisão." }

        return when (splits.first()) {
            is ExpenseSplit.Equal -> dividirIgualmente(total, splits)
            is ExpenseSplit.Weight -> dividirPorPeso(total, splits)
            is ExpenseSplit.FixedAmount -> dividirPorValorFixo(splits)
        }
    }

    private fun dividirIgualmente(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        val ordenados = splits.map { it.participantId }.sorted()

        val quantidade = ordenados.size
        val base = total.cents / quantidade
        val resto = total.cents % quantidade

        return ordenados.mapIndexed { indice, participantId ->
            val centavoExtra = if (indice < resto) 1 else 0
            participantId to Money.ofCents(base + centavoExtra)
        }.toMap()
    }

    private fun dividirPorPeso(total: Money, splits: List<ExpenseSplit>): Map<String, Money> {
        val pesos = splits.filterIsInstance<ExpenseSplit.Weight>()
        val somaPesos = pesos.sumOf { it.weight }

        val parteBasePorId = pesos.associate { it.participantId to (total.cents * it.weight / somaPesos) }
        val restoFracionarioPorId = pesos.associate { it.participantId to (total.cents * it.weight % somaPesos) }

        val centavosNaoDistribuidos = total.cents - parteBasePorId.values.sum()
        val ordemDeDesempate = pesos
            .map { it.participantId }
            .sortedWith(compareByDescending<String> { restoFracionarioPorId.getValue(it) }.thenBy { it })

        return ordemDeDesempate.mapIndexed { indice, participantId ->
            val centavoExtra = if (indice < centavosNaoDistribuidos) 1 else 0
            participantId to Money.ofCents(parteBasePorId.getValue(participantId) + centavoExtra)
        }.toMap()
    }

    private fun dividirPorValorFixo(splits: List<ExpenseSplit>): Map<String, Money> =
        splits.filterIsInstance<ExpenseSplit.FixedAmount>()
            .associate { it.participantId to it.amount }

    // --- computeSettlement (algoritmo guloso) ---------------------------------------------

    private fun quitarMaiorDevedorComMaiorCredor(
        devedores: MutableList<ParticipanteComSaldo>,
        credores: MutableList<ParticipanteComSaldo>,
    ): SettlementSuggestion {
        val devedor = removerMaiorDevedorOuCredor(devedores)
        val credor = removerMaiorDevedorOuCredor(credores)

        val valorQuitado = menorValor(devedor.restante, credor.restante)

        devolverSeAindaTemSaldo(devedores, devedor.comRestante(devedor.restante - valorQuitado))
        devolverSeAindaTemSaldo(credores, credor.comRestante(credor.restante - valorQuitado))

        return SettlementSuggestion(devedor.id, credor.id, valorQuitado)
    }

    /**
     * Acha e remove o maior saldo restante da lista: valor desc, empate por participantId asc.
     * Reselecionar o maior a cada chamada (em vez de ordenar uma vez e percorrer com dois
     * ponteiros) é intencional — ver algorithm-spec.md, seção computeSettlement: não é uma
     * otimização válida do pseudocódigo, é um algoritmo diferente que pode gerar uma transação a
     * mais em alguns casos.
     */
    private fun removerMaiorDevedorOuCredor(participantes: MutableList<ParticipanteComSaldo>): ParticipanteComSaldo {
        val maior = participantes
            .sortedWith(compareByDescending<ParticipanteComSaldo> { it.restante }.thenBy { it.id })
            .first()

        participantes.remove(maior)
        return maior
    }

    private fun devolverSeAindaTemSaldo(participantes: MutableList<ParticipanteComSaldo>, participante: ParticipanteComSaldo) {
        if (!participante.restante.isPositive) return
        participantes.add(participante)
    }

    private fun menorValor(a: Money, b: Money): Money = if (a <= b) a else b

    private fun participantesComSaldoNegativo(saldos: Map<String, Money>): List<ParticipanteComSaldo> =
        saldos.filterValues { it.isNegative }.map { (id, saldo) -> ParticipanteComSaldo(id, -saldo) }

    private fun participantesComSaldoPositivo(saldos: Map<String, Money>): List<ParticipanteComSaldo> =
        saldos.filterValues { it.isPositive }.map { (id, saldo) -> ParticipanteComSaldo(id, saldo) }

    /**
     * Participante com o valor (sempre positivo) que ainda falta quitar na fila de devedores ou
     * de credores do algoritmo guloso. Espelha `ParticipanteComSaldo`, o record privado
     * equivalente em `MotorDeSimplificacaoDeDividas.cs`.
     */
    private data class ParticipanteComSaldo(val id: String, val restante: Money) {
        fun comRestante(novoRestante: Money): ParticipanteComSaldo = copy(restante = novoRestante)
    }
}
