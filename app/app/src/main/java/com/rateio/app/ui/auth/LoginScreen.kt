package com.rateio.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.R
import com.rateio.app.ui.groups.RateioBottomBar
import com.rateio.app.ui.groups.RateioBottomTab
import com.rateio.app.ui.theme.LocalRateioColors
import com.rateio.app.ui.theme.ThemeToggleButton
import com.rateio.domain.model.AuthenticatedUser

/**
 * Ponto de entrada da tela de login (T12.1), fiel a `prototype/login.html`. `factory` injeta o
 * [AuthViewModel] pela composição manual de [com.rateio.app.di.AppContainer] — mesma convenção de
 * [com.rateio.app.ui.groups.GroupListRoute] (T8): a Composable em si não sabe de onde vem o
 * estado nem como o ID token do Google é obtido.
 *
 * [onSignedIn] (T22.1) é opcional: quando não nulo, dispara uma única vez assim que [uiState] vira
 * [AuthUiState.SignedIn], via [LaunchedEffect]. Usado pelo fluxo "entrar no grupo por link" pra
 * retomar automaticamente depois do login — o acesso normal à tela (ícone de perfil) não passa
 * esse parâmetro e continua sem navegação automática após logar.
 */
@Composable
fun LoginRoute(
    factory: AuthViewModelFactory,
    onBackClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onSignedIn: (() -> Unit)? = null,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (onSignedIn != null && uiState is AuthUiState.SignedIn) onSignedIn()
    }

    LoginScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSignInClick = viewModel::signInWithGoogle,
        onSignOutClick = viewModel::signOut,
        onContinueWithoutAccount = onContinueWithoutAccount,
        unreadNotificationsCount = unreadNotificationsCount,
        onGroupsClick = onGroupsClick,
        onNotificationsClick = onNotificationsClick,
        authenticatedUserName = authenticatedUserName,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

/**
 * Composable fina: escolhe entre [SignInContent] e [AccountContent] a partir do [uiState] e
 * sobrepõe [ConnectingOverlay] enquanto conecta — réplica de `#screen` + `#overlay` de
 * `prototype/login.html` (o overlay some por cima da tela em vez de substituí-la).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            LoginTopBar(
                uiState = uiState,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        bottomBar = {
            RateioBottomBar(
                selectedTab = RateioBottomTab.PERFIL,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        RateioBottomTab.GRUPOS -> onGroupsClick()
                        RateioBottomTab.AVISOS -> onNotificationsClick()
                        RateioBottomTab.PERFIL -> Unit
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is AuthUiState.SignedIn -> AccountContent(user = uiState.user, onSignOutClick = onSignOutClick)
                is AuthUiState.SignedOut -> SignInContent(
                    errorMessage = uiState.errorMessage,
                    onSignInClick = onSignInClick,
                    onContinueWithoutAccount = onContinueWithoutAccount,
                )
                AuthUiState.Connecting -> SignInContent(
                    errorMessage = null,
                    onSignInClick = onSignInClick,
                    onContinueWithoutAccount = onContinueWithoutAccount,
                )
            }
            if (uiState is AuthUiState.Connecting) ConnectingOverlay()
        }
    }
}

/** `.topbar h1` do protótipo — "Entrar" deslogado/conectando, "Sua conta" logado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTopBar(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalRateioColors.current
    val title = if (uiState is AuthUiState.SignedIn) "Sua conta" else "Entrar"
    TopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = { ThemeToggleButton(isDarkTheme = isDarkTheme, onToggleClick = onToggleTheme) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            actionIconContentColor = colors.ink,
        ),
    )
}

/** Estado deslogado: `.card` explicativo + `.btn-google` + `.btn-ghost` "Continuar sem conta". */
@Composable
private fun SignInContent(
    errorMessage: String?,
    onSignInClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        SyncExplanationCard()
        Spacer(Modifier.height(16.dp))
        GoogleSignInButton(onClick = onSignInClick)
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = errorMessage, color = colors.danger, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(4.dp))
        ContinueWithoutAccountLink(onClick = onContinueWithoutAccount)
        Spacer(Modifier.height(18.dp))
    }
}

/** `.card` do protótipo: três motivos pra sincronizar, nenhum obrigatório (princípio 1). */
@Composable
private fun SyncExplanationCard(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.paperAlt, shape = RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        ExplanationLine(text = "Usar o Rateio não exige conta. Entrar com Google é só pra:")
        ExplanationLine(text = "· manter um grupo disponível em mais de um aparelho")
        ExplanationLine(text = "· avisar em tempo real quando alguém no grupo lança despesa ou quita dívida")
        ExplanationLine(text = "· guardar o histórico de grupos anteriores", isLast = true)
    }
}

@Composable
private fun ExplanationLine(text: String, isLast: Boolean = false, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Text(
        text = text,
        color = colors.inkSoft,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        modifier = modifier.padding(bottom = if (isLast) 0.dp else 10.dp),
    )
}

/** `.btn-google` do protótipo: fundo branco fixo (não segue tema), logo colorido + rótulo. */
@Composable
private fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, colors.rule),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = "Entrar com Google", color = GoogleButtonTextColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** `.btn-ghost` do protótipo — sempre visível, nunca bloqueado por login (princípio 1). */
@Composable
private fun ContinueWithoutAccountLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text = "Continuar sem conta", color = colors.inkSoft, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** `#overlay` do protótipo: scrim + spinner sobre a tela, sem trocar de tela. */
@Composable
private fun ConnectingOverlay(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper.copy(alpha = OVERLAY_SCRIM_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colors.brandInk, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(12.dp))
            Text(text = "Conectando ao Google…", color = colors.inkSoft, fontSize = 13.sp)
        }
    }
}

/** Estado logado: bloco "Sua conta" (avatar + nome + email), explicação e `.btn-secondary` "Sair". */
@Composable
private fun AccountContent(user: AuthenticatedUser, onSignOutClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        AccountCard(user = user)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Grupos sincronizados ficam disponíveis em qualquer aparelho com essa conta, " +
                "e você recebe aviso em tempo real quando alguém lança despesa ou quita dívida.",
            color = colors.inkSoft,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(16.dp))
        SignOutButton(onClick = onSignOutClick)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccountCard(user: AuthenticatedUser, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.paperAlt, shape = RoundedCornerShape(14.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(color = colors.brand, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initialsOf(user.name), color = colors.onBrand, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = user.name, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = user.email, color = colors.inkSoft, fontSize = 13.5.sp)
    }
}

/**
 * `.btn-secondary` do protótipo. Revoga a sessão no backend e limpa a sessão guardada no
 * aparelho (T14.2, via `AuthViewModel.signOut` → `RemoteAuthRepository.signOut`) — a limpeza
 * local acontece sempre, mesmo se a revogação no backend falhar por falta de rede.
 */
@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.paperAlt,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(text = "Sair", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** Mesma heurística de `initials()` em `prototype/app.js`: até duas iniciais, maiúsculas. */
private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { word -> word.isNotBlank() }
        .take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")

private val GoogleButtonTextColor = Color(0xFF211F1B)
private const val OVERLAY_SCRIM_ALPHA = 0.88f
