package com.rateio.app.ui.createexpense

import com.rateio.domain.model.Money

/**
 * Divide [total] em partes iguais entre [participantIds], fechando o resto pela mesma regra de
 * maiores restos descrita em `algorithm-spec.md` ("Fechando divisões que não dão exato") e
 * implementada em `GreedyDebtSimplificationEngine.dividirIgualmente` (T33): ordena por
 * `participantId` ascendente, `base = total / n`, `resto = total % n`, e os primeiros `resto`
 * participantes (nessa ordem) recebem um centavo a mais — nunca `total.cents / n` ingênuo, que
 * perde centavo (ex.: R$10,00 ÷ 3 = 333+333+333 = 999 ≠ 1000).
 *
 * Reimplementada aqui — função pequena, pura e testável isoladamente (Object Calisthenics: sem
 * cálculo inline na Composable) — em vez de chamada direta ao motor porque `dividirIgualmente` é
 * privada a [com.rateio.domain.engine.GreedyDebtSimplificationEngine] e esta task (T24) está
 * restrita a `app/app/`; a regra é a mesma, linha a linha, só o participante fica responsável por
 * calcular o preview ao vivo do formulário, não o saldo do grupo (isso é T25/T33).
 */
fun calculateEqualSplit(total: Money, participantIds: List<String>): Map<String, Money> {
    if (participantIds.isEmpty()) return emptyMap()

    val ordenados = participantIds.sorted()
    val quantidade = ordenados.size
    val base = total.cents / quantidade
    val resto = total.cents % quantidade

    return ordenados.mapIndexed { indice, participantId ->
        val centavoExtra = if (indice < resto) 1 else 0
        participantId to Money.ofCents(base + centavoExtra)
    }.toMap()
}
