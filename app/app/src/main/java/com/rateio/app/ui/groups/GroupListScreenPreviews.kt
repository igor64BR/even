package com.rateio.app.ui.groups

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rateio.app.ui.theme.RateioTheme

/**
 * Visual validation for T8.1/T8.2 without an emulator (recorded in the task's report): mocked
 * data covering the prototype's three balance states (owed to you/you owe/settled), local vs.
 * synced group, list with data, empty state, and both themes — compared hex-by-hex against
 * `prototype/styles.css` and layout against `prototype/index.html`.
 */
private val previewGroups = listOf(
    GroupListItemUiModel(
        id = "bbq",
        name = "Saturday BBQ",
        tag = "SA",
        participantCount = 4,
        isSynced = false,
        balance = GroupBalance.YouOwe(amountCents = 4_500),
    ),
    GroupListItemUiModel(
        id = "beach",
        name = "Beach trip",
        tag = "BE",
        participantCount = 3,
        isSynced = true,
        balance = GroupBalance.YouAreOwed(amountCents = 12_000),
    ),
    GroupListItemUiModel(
        id = "roommates",
        name = "Roommates — October bills",
        tag = "RO",
        participantCount = 2,
        isSynced = false,
        balance = GroupBalance.Settled,
    ),
)

@Preview(name = "List — light", showBackground = true)
@Composable
private fun GroupListScreenContentLightPreview() {
    RateioTheme(darkTheme = false) {
        GroupListScreen(
            uiState = GroupListUiState.Content(previewGroups),
            onCreateGroupClick = {},
            onGroupClick = {},
        )
    }
}

@Preview(name = "List — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupListScreenContentDarkPreview() {
    RateioTheme(darkTheme = true) {
        GroupListScreen(
            uiState = GroupListUiState.Content(previewGroups),
            onCreateGroupClick = {},
            onGroupClick = {},
        )
    }
}

@Preview(name = "Empty state — light", showBackground = true)
@Composable
private fun GroupListScreenEmptyLightPreview() {
    RateioTheme(darkTheme = false) {
        GroupListScreen(
            uiState = GroupListUiState.Empty,
            onCreateGroupClick = {},
            onGroupClick = {},
        )
    }
}

@Preview(name = "Empty state — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GroupListScreenEmptyDarkPreview() {
    RateioTheme(darkTheme = true) {
        GroupListScreen(
            uiState = GroupListUiState.Empty,
            onCreateGroupClick = {},
            onGroupClick = {},
        )
    }
}
