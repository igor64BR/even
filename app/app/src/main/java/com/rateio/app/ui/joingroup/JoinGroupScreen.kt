package com.rateio.app.ui.joingroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * Ponto de entrada do fluxo "entrar no grupo por link" (T22.2). `factory` injeta o
 * [JoinGroupViewModel] pela composição manual de [com.rateio.app.di.AppContainer] — mesma
 * convenção de [com.rateio.app.ui.auth.LoginRoute] (T12).
 *
 * [onNeedsLogin] é chamado (via [LaunchedEffect]) quando [JoinGroupUiState.NeedsLogin] aparece —
 * quem monta a navegação (`MainActivity`) decide o que fazer (mandar pra tela de login guardando
 * o código pra retomar depois). Esta Composable não navega sozinha.
 */
@Composable
fun JoinGroupRoute(
    factory: JoinGroupViewModelFactory,
    onNeedsLogin: (inviteCode: String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: JoinGroupViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val needsLogin = uiState as? JoinGroupUiState.NeedsLogin ?: return@LaunchedEffect
        onNeedsLogin(needsLogin.inviteCode)
    }

    JoinGroupScreen(
        uiState = uiState,
        onConfirmClick = viewModel::confirm,
        onRetryClick = viewModel::retry,
        onCancelClick = onDone,
        onDoneClick = onDone,
        modifier = modifier,
    )
}

/**
 * Composable fina: escolhe o conteúdo a partir do [uiState]. [JoinGroupUiState.NeedsLogin] e
 * [JoinGroupUiState.CheckingSession] renderizam só um carregando — [JoinGroupRoute] já navega pra
 * fora assim que [JoinGroupUiState.NeedsLogin] aparece, então esses dois estados só aparecem por
 * um instante (mesmo comportamento tolerado em [com.rateio.app.ui.auth.AuthViewModel] antes da
 * primeira emissão de sessão).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    uiState: JoinGroupUiState,
    onConfirmClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = { Text(text = "Entrar no grupo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper, titleContentColor = colors.ink),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            when (uiState) {
                JoinGroupUiState.CheckingSession, is JoinGroupUiState.NeedsLogin -> LoadingContent()
                is JoinGroupUiState.Confirming -> ConfirmContent(onConfirmClick = onConfirmClick, onCancelClick = onCancelClick)
                JoinGroupUiState.Joining -> LoadingContent()
                is JoinGroupUiState.Success -> ResultContent(
                    message = "Você entrou no grupo. Sincronize novamente o app pra ver os detalhes " +
                        "completos assim que essa parte do backend existir.",
                    onDoneClick = onDoneClick,
                )
                is JoinGroupUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetryClick = onRetryClick,
                    onCancelClick = onCancelClick,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    CircularProgressIndicator(color = colors.brandInk, strokeWidth = 3.dp, modifier = modifier.size(28.dp))
}

/**
 * "Você foi convidado a entrar em um grupo" — texto genérico de propósito: T21 não tem endpoint
 * de preview do grupo pelo código, então não há nome pra mostrar aqui (ver
 * `T22-app-entrar-via-link.md`, "não invente").
 */
@Composable
private fun ConfirmContent(onConfirmClick: () -> Unit, onCancelClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colors.paperAlt,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Você foi convidado a entrar em um grupo",
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
        Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onConfirmClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Entrar no grupo")
            }
            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        }
    }
}

@Composable
private fun ResultContent(message: String, onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = colors.ink, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(onClick = onDoneClick, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(text = "OK")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = colors.danger,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Column(
            Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onRetryClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Tentar de novo")
            }
            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        }
    }
}
