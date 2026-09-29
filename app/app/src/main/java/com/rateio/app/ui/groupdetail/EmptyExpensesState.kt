package com.rateio.app.ui.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.theme.LocalRateioColors

/** `group.html`'s `.empty-state` when the group's expenses are empty. */
@Composable
fun EmptyExpensesState(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "No expenses logged yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
        )
        Text(
            text = "Log the first expense and each participant's balance shows up here.",
            fontSize = 13.5.sp,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
