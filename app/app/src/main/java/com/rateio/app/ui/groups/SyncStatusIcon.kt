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
 * The prototype's `.sync-icon` (`app.js`, `syncIcon()`): filled cloud for a synced group, crossed
 * cloud (outline + slash) for a local-only group. `tint` is always `--ink-soft`, resolved by the
 * caller via [LocalContentColor][androidx.compose.material3.LocalContentColor].
 */
@Composable
fun SyncStatusIcon(isSynced: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (isSynced) Icons.Filled.Cloud else Icons.Filled.CloudOff,
        contentDescription = if (isSynced) "Synced" else "Local — this device only",
        modifier = modifier.size(16.dp),
    )
}
