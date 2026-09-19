package com.rateio.app.ui.settledebts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

/** `.empty-state` de `quitar.html` quando `transacoes` está vazia — "grupo quitado" (RF44). */
@Composable
fun SettledUpState(groupName: String, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = colors.rule)
        Text(text = "Grupo quitado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(
            text = "Ninguém deve nada a ninguém em \"$groupName\".",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
