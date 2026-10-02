package com.even.app.ui.settledebts

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
import com.even.app.ui.theme.LocalEvenColors

/** `.empty-state` from `settle.html` when `transactions` is empty — "group settled up". */
@Composable
fun SettledUpState(groupName: String, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 30.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = colors.neutral)
        Text(text = "All settled up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)
        Text(
            text = "No one owes anyone in \"$groupName\".",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
