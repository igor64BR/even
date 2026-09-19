package com.rateio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rateio.app.di.AppContainer
import com.rateio.app.ui.auth.AuthViewModelFactory
import com.rateio.app.ui.auth.LoginRoute
import com.rateio.app.ui.groups.GroupListRoute
import com.rateio.app.ui.groups.GroupListViewModelFactory
import com.rateio.app.ui.theme.RateioTheme

/**
 * Ponto de entrada do app (RF40 — abre direto em "Seus grupos", sem login). Só monta o tema e
 * delega pra [GroupListRoute]; nenhuma lógica de UI mora aqui.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RateioApplication).container
        setContent {
            RateioApp(container = container)
        }
    }
}

/**
 * T12 — ainda não há um NavHost no app (T9, bottom nav de verdade, não existe); esta troca de
 * tela é uma máquina de estados mínima, só pra abrir "Seus grupos" ↔ "Entrar"/"Sua conta" sem
 * antecipar a estrutura de navegação que outra task vai introduzir.
 */
@Composable
private fun RateioApp(container: AppContainer) {
    val groupListViewModelFactory = GroupListViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )
    val authViewModelFactory = AuthViewModelFactory(
        authRepository = container.authRepository,
        googleIdentityClient = container.googleIdentityClient,
    )
    var showLogin by remember { mutableStateOf(false) }

    RateioTheme {
        if (showLogin) {
            LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { showLogin = false },
                onContinueWithoutAccount = { showLogin = false },
            )
        } else {
            GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { /* T16 — tela "Novo grupo" ainda não existe. */ },
                onGroupClick = { /* T-detalhe de grupo (RF42) ainda não existe. */ },
                onProfileClick = { showLogin = true },
            )
        }
    }
}
