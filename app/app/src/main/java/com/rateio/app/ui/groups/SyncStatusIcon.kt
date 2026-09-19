package com.rateio.app.ui.groups

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * `.sync-icon` do protótipo (`app.js`, `syncIcon()`): nuvem cheia (preenchida) para grupo
 * sincronizado, nuvem cortada (contorno + traço) para grupo só local. `tint` é sempre
 * `--ink-soft`, resolvido pelo chamador via [LocalContentColor][androidx.compose.material3.LocalContentColor].
 */
@Composable
fun SyncStatusIcon(isSynced: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (isSynced) Icons.Filled.Cloud else Icons.Filled.CloudOff,
        contentDescription = if (isSynced) "Sincronizado" else "Local — só neste aparelho",
        modifier = modifier.size(16.dp),
    )
}
