package com.rateio.app.ui.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.format.formatCentsAsBrl
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `.expense-row` de `grupo.html`: ícone, descrição + "quem pagou · data · tipo de divisão", valor
 * total à direita. Puramente apresentação — [expense] já vem com tudo formatado
 * ([GroupDetailViewModel]).
 *
 * T29.1: dois gestos, escolhidos pelo mais simples/discoverable pra cada ação (o protótipo não
 * detalha esse fluxo, ver T29-app-editar-excluir-despesa.md) — tocar em qualquer parte da linha
 * chama [onClick] (abre "Editar despesa" pré-preenchida, T24/T26 reaproveitado em modo edição);
 * o ícone de lixeira chama [onDeleteClick], que só *pede* confirmação — [GroupDetailScreen] é
 * quem decide excluir de fato depois do diálogo, [ExpenseRow] nunca exclui nada sozinho.
 */
@Composable
fun ExpenseRow(
    expense: ExpenseRowUiModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = colors.paperAlt, shape = RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Receipt, contentDescription = null, tint = colors.inkSoft)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.description, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.ink)
            Text(
                text = "${expense.payerName} pagou · ${expense.dateLabel} · ${expense.splitTypeLabel}",
                fontSize = 12.sp,
                color = colors.inkSoft,
            )
        }
        Text(
            text = formatCentsAsBrl(expense.amountCents),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.ink,
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Excluir despesa",
                tint = colors.inkSoft,
            )
        }
    }
}
