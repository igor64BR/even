package com.tally.app.ui.groups

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
import com.tally.app.ui.theme.LocalTallyColors

/**
 * The prototype's `.empty-state` (`index.html?vazio=1`): receipt icon, title, text explaining that
 * no account is needed, "Create group" button. Nothing to list when there are no groups.
 */
@Composable
fun EmptyGroupsState(onCreateGroupClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTallyColors.current

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
            text = "No groups yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
        )
        Text(
            text = "Creating one takes 10 seconds. No account needed — not yours, not the " +
                "people splitting the bill with you.",
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
            Text(text = "Create group", fontWeight = FontWeight.SemiBold)
        }
    }
}
