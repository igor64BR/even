package com.rateio.app.ui.groups

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rateio.app.ui.theme.RateioTheme

/**
 * Validação visual de T8.1/T8.2 sem emulador (registrado no relatório da task): dados mockados
 * cobrindo os três estados de saldo do protótipo (a receber/a dever/quitado), grupo local vs.
 * sincronizado, lista com dados, estado vazio, e os dois temas — comparados hex a hex contra
 * `prototype/styles.css` e layout contra `prototype/index.html`.
 */
private val previewGroups = listOf(
    GroupListItemUiModel(
        id = "churras",
        name = "Churras de sábado",
        tag = "CH",
        participantCount = 4,
        isSynced = false,
        balance = GroupBalance.YouOwe(amountCents = 4_500),
    ),
    GroupListItemUiModel(
        id = "praia",
        name = "Viagem pra praia",
        tag = "VI",
        participantCount = 3,
        isSynced = true,
        balance = GroupBalance.YouAreOwed(amountCents = 12_000),
    ),
    GroupListItemUiModel(
        id = "republica",
        name = "República — contas de outubro",
        tag = "RE",
        participantCount = 2,
        isSynced = false,
        balance = GroupBalance.Settled,
    ),
)

@Preview(name = "Lista — claro", showBackground = true)
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

@Preview(name = "Lista — escuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

@Preview(name = "Estado vazio — claro", showBackground = true)
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

@Preview(name = "Estado vazio — escuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
