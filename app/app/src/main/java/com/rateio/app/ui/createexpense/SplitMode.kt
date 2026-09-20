package com.rateio.app.ui.createexpense

/**
 * Aba ativa em "Como dividir" (`#tabs` do protótipo: Igual/Percentual/Valor fixo) — puramente um
 * detalhe de UI de formulário, não confundir com a hierarquia de domínio [ExpenseSplit][com
 * .rateio.domain.model.ExpenseSplit]. Lá, deliberadamente, não existe um enum solto (ver o
 * comentário de `ExpenseSplit.kt`: o subtipo concreto já carrega a regra de divisão, um enum ao
 * lado poderia divergir do subtipo real de cada participação). Aqui é diferente: antes de salvar,
 * a tela só tem *um* conjunto de participantes com inputs por modo (checkbox / % / R$) — precisa
 * de algo pra saber qual conjunto de campos renderizar e validar, e isso nunca persiste — é
 * traduzido para o subtipo [com.rateio.domain.model.ExpenseSplit] certo só no momento de salvar
 * (ver `CreateExpenseViewModel.buildSplits`).
 */
enum class SplitMode {
    EQUAL,
    PERCENTAGE,
    FIXED_AMOUNT,
}
