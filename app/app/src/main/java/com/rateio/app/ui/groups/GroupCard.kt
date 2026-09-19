package com.rateio.app.ui.groups

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `.group-card` do protótipo: sigla do grupo, nome + contagem de participantes + ícone de sync,
 * saldo colorido à direita. Uma responsabilidade — apresentar um [GroupListItemUiModel]; nenhuma
 * lógica de saldo/sync mora aqui (isso é do ViewModel).
 *
 * [onClick] leva pra "Detalhes do grupo" (T42.4/RF42). Até T19 este card também embutia a ação
 * "Sincronizar este grupo" (atalho temporário, porque a tela de detalhe não existia ainda); T42.4
 * remove esse atalho — sincronizar agora é uma ação dentro da tela de detalhe.
 */
@Composable
fun GroupCard(
    group: GroupListItemUiModel,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(group.id) }
            .padding(vertical = 13.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GroupTag(text = group.tag)
        GroupNameAndMeta(
            name = group.name,
            participantCount = group.participantCount,
            isSynced = group.isSynced,
            modifier = Modifier.weight(1f),
        )
        GroupBalanceLabel(balance = group.balance)
    }
}

@Composable
private fun GroupTag(text: String, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = colors.paperAlt, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.brandInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun GroupNameAndMeta(
    name: String,
    participantCount: Int,
    isSynced: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier) {
        Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$participantCount ${if (participantCount == 1) "pessoa" else "pessoas"}",
                color = colors.inkSoft,
                fontSize = 12.5.sp,
            )
            SyncStatusIcon(isSynced = isSynced)
        }
    }
}
