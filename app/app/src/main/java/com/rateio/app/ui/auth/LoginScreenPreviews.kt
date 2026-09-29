package com.rateio.app.ui.auth

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rateio.app.ui.theme.RateioTheme
import com.rateio.domain.model.AuthenticatedUser

/**
 * Visual validation of T12.1 with no emulator (same rationale as `GroupListScreenPreviews.kt`,
 * T8): the three [AuthUiState] states in both themes, compared against `prototype/login.html`.
 */
@Preview(name = "Signed out — light", showBackground = true)
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

@Preview(name = "Signed out — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

@Preview(name = "Signed out — with error", showBackground = true)
@Composable
private fun LoginScreenSignedOutErrorPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.SignedOut(errorMessage = "Google didn't confirm that account."),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Connecting", showBackground = true)
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

@Preview(name = "Your account — light", showBackground = true)
@Composable
private fun LoginScreenSignedInLightPreview() {
    RateioTheme(darkTheme = false) {
        LoginScreen(
            uiState = AuthUiState.SignedIn(AuthenticatedUser(name = "You", email = "you@gmail.com")),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}

@Preview(name = "Your account — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenSignedInDarkPreview() {
    RateioTheme(darkTheme = true) {
        LoginScreen(
            uiState = AuthUiState.SignedIn(AuthenticatedUser(name = "You", email = "you@gmail.com")),
            onBackClick = {},
            onSignInClick = {},
            onSignOutClick = {},
            onContinueWithoutAccount = {},
        )
    }
}
