package com.rateio.app.ui.createexpense

/**
 * Aba "Percentual" (T26.1): cada linha guarda um `%` inteiro digitado livremente (mesmo `<input
 * type="number" min="0" max="100">` do protótipo — os atributos min/max não bloqueiam nada no
 * JavaScript, só a soma final é validada), e salvar só é permitido quando a soma bate exatamente
 * 100 (`prototype/nova-despesa.html`, `updateSplitSum`/handler de `salvar-btn`: usa uma tolerância
 * de 0,5 porque lá os percentuais podem ter casas decimais; aqui o percentual é sempre um `Long`
 * inteiro — [com.rateio.domain.model.ExpenseSplit.Weight.weight] também é `Long` — então a soma é
 * comparada com igualdade exata, sem tolerância).
 *
 * Funções pequenas e puras (Object Calisthenics), testáveis isoladamente — nenhuma soma nem
 * parsing mora nas Composables de percentual.
 */

/** `null` para entrada vazia/inválida/negativa — mesma convenção de [parseAmountInputToCents]. */
fun parsePercentageInput(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.toLongOrNull()?.takeIf { it >= 0 }
}

/** Percentual padrão ao carregar a aba: `Math.round(100 / n)`, igual ao protótipo. Não fecha 100%
 * sozinho quando `n` não divide 100 exatamente — o usuário precisa ajustar, de propósito, mesmo
 * comportamento de lá. */
fun defaultPercentage(participantCount: Int): Long {
    if (participantCount <= 0) return 0L
    return Math.round(100.0 / participantCount)
}

/** Soma dos percentuais de todas as linhas; entrada inválida/vazia conta como 0. */
fun sumPercentages(rows: List<ExpenseSplitRowUiModel>): Long =
    rows.sumOf { parsePercentageInput(it.percentageInput) ?: 0L }

/** Gate de "Salvar despesa" na aba Percentual: soma precisa fechar 100% exatos. */
fun isPercentageSplitComplete(rows: List<ExpenseSplitRowUiModel>): Boolean =
    sumPercentages(rows) == FULL_PERCENTAGE

private const val FULL_PERCENTAGE = 100L
