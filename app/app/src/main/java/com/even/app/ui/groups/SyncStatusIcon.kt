package com.even.app.ui.groups

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.even.app.ui.theme.LocalEvenColors

/**
 * The prototype's `.sync-icon` (`app.js`, `syncIcon()`): filled cloud for a synced group, crossed
 * cloud (outline + slash) for a local-only group. `.sync-icon { color: var(--ink-soft) }` — it sits
 * in the metadata line, so it's tinted like metadata, not like the ink around it.
 */
@Composable
fun SyncStatusIcon(isSynced: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (isSynced) Icons.Filled.Cloud else Icons.Filled.CloudOff,
        contentDescription = if (isSynced) "Synced" else "Local — this device only",
        tint = LocalEvenColors.current.inkSoft,
        modifier = modifier.size(16.dp),
    )
}
