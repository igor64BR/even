package com.rateio.app.ui.auth

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rateio.app.ui.theme.RateioTheme
import com.rateio.domain.model.AuthenticatedUser

/**
 * Validação visual de T12.1 sem emulador (mesmo racional de `GroupListScreenPreviews.kt`, T8):
 * os três estados de [AuthUiState] nos dois temas, comparados contra `prototype/login.html`.
 */
@Preview(name = "Deslogado — claro", showBackground = true)
@Composable
private fun LoginScreenSignedOutLightPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.SignedOut(),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Deslogado — escuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenSignedOutDarkPreview() {
    RateioTheme(darkTheme = true) {
        LoginScreen(
            uiState = AuthUiState.SignedOut(),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Deslogado — com erro", showBackground = true)
@Composable
private fun LoginScreenSignedOutErrorPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.SignedOut(errorMessage = "O Google não confirmou essa conta."),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Conectando", showBackground = true)
@Composable
private fun LoginScreenConnectingPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.Connecting,
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Sua conta — claro", showBackground = true)
@Composable
private fun LoginScreenSignedInLightPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.SignedIn(AuthenticatedUser(name = "Você", email = "voce@gmail.com")),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Sua conta — escuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenSignedInDarkPreview() {
    RateioTheme(darkTheme = true) {
        LoginScreen(
            uiState = AuthUiState.SignedIn(AuthenticatedUser(name = "Você", email = "voce@gmail.com")),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}
