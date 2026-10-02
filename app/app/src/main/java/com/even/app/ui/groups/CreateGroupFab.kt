package com.even.app.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.even.app.ui.theme.LocalEvenColors

/** The prototype's `.fab`: a `--brand-ink` circle, white "add person" icon. */
@Composable
fun CreateGroupFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEvenColors.current
    FloatingActionButton(
        onClick = onClick,
        containerColor = colors.brand,
        contentColor = colors.onBrand,
        modifier = modifier,
    ) {
        Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = "Create group")
    }
}
