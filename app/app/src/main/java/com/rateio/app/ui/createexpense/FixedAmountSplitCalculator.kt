package com.rateio.app.ui.createexpense

import com.rateio.app.ui.format.parseAmountInputToCents
import com.rateio.domain.model.Money

/**
 * Aba "Valor fixo" (T26.2): cada linha guarda um valor em R$ digitado livremente (mesmo `<input
 * type="number" step="0.01">` do protótipo), e salvar só é permitido quando a soma bate exatamente
 * o valor total da despesa (`prototype/nova-despesa.html`, `updateSplitSum`: tolerância de 0,01 lá
 * porque o valor é `parseFloat`; aqui tudo é [Money]/centavos inteiros, então a comparação é
 * igualdade exata, sem tolerância de ponto flutuante).
 *
 * Reaproveita [parseAmountInputToCents] — mesmo parser de borda do campo "Valor total"
 * ([AmountField]) — em vez de reinventar parsing de dinheiro (T7B: dinheiro nunca `Double`/`Float`
 * cru fora da borda de entrada).
 */

/** `null` para entrada vazia/inválida/negativa. */
fun parseFixedAmountInput(input: String): Money? {
    val cents = parseAmountInputToCents(input) ?: return null
    return Money.ofCents(cents)
}

/** Soma dos valores fixos de todas as linhas; entrada inválida/vazia conta como zero. */
fun sumFixedAmounts(rows: List<ExpenseSplitRowUiModel>): Money =
    Money.ofCents(rows.sumOf { parseFixedAmountInput(it.fixedAmountInput)?.cents ?: 0L })

/** Gate de "Salvar despesa" na aba Valor fixo: soma precisa fechar o valor total exato. */
fun isFixedAmountSplitComplete(rows: List<ExpenseSplitRowUiModel>, total: Money): Boolean =
    sumFixedAmounts(rows) == total
