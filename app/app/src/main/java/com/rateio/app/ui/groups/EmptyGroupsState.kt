package com.rateio.app.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * `.empty-state` do protótipo (`index.html?vazio=1`): ícone de recibo, título, texto explicando
 * que não precisa de conta, botão "Criar grupo". Sem lista para mostrar quando não há grupos.
 */
@Composable
fun EmptyGroupsState(onCreateGroupClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Receipt,
            contentDescription = null,
            tint = colors.rule,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Nenhum grupo ainda",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
        )
        Text(
            text = "Criar um leva 10 segundos. Não precisa de conta — nem sua, nem de quem vai " +
                "dividir a conta com você.",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Button(
            onClick = onCreateGroupClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandInk,
                contentColor = colors.onBrand,
            ),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(text = "Criar grupo", fontWeight = FontWeight.SemiBold)
        }
    }
}
