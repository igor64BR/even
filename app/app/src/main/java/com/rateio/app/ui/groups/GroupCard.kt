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
 * [onSyncClick] (T19) é a ação "Sincronizar este grupo" embutida no próprio card — não existe
 * tela de detalhe de grupo ainda (RF42), então este é o lugar mais próximo de "onde o usuário já
 * está olhando pro grupo" que existe hoje; ver [SyncAction].
 */
@Composable
fun GroupCard(
    group: GroupListItemUiModel,
    onClick: (String) -> Unit,
    onSyncClick: (String) -> Unit,
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
        Column(horizontalAlignment = Alignment.End) {
            GroupBalanceLabel(balance = group.balance)
            SyncAction(state = group.syncAction, onClick = { onSyncClick(group.id) })
        }
    }
}

/**
 * Botão temporário simples (T19, registrado na task: "pode ser um botão temporário simples se
 * ainda não houver tela de detalhe de grupo" — RF42 não existe). [GroupSyncActionUiState.Hidden]
 * não desenha nada, então grupos já sincronizados ou usuário deslogado não ganham espaço extra no
 * card. Em [GroupSyncActionUiState.Failed] o próprio texto de erro é o convite pra tentar de novo
 * (tocável) — o grupo continua local (`isSynced=false`), nenhum estado fica inconsistente.
 */
@Composable
private fun SyncAction(state: GroupSyncActionUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    when (state) {
        GroupSyncActionUiState.Hidden -> Unit

        GroupSyncActionUiState.Available -> Text(
            text = "Sincronizar",
            color = colors.brand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            modifier = modifier.clickable(onClick = onClick),
        )

        GroupSyncActionUiState.InProgress -> Text(
            text = "Sincronizando…",
            color = colors.inkSoft,
            fontSize = 11.5.sp,
            modifier = modifier,
        )

        is GroupSyncActionUiState.Failed -> Text(
            text = "Falhou — tentar de novo",
            color = colors.danger,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            modifier = modifier.clickable(onClick = onClick),
        )
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
