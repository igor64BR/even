package com.rateio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.rateio.app.di.AppContainer
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

@Composable
private fun RateioApp(container: AppContainer) {
    val groupListViewModelFactory = GroupListViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )

    RateioTheme {
        GroupListRoute(
            factory = groupListViewModelFactory,
            onCreateGroupClick = { /* T16 — tela "Novo grupo" ainda não existe. */ },
            onGroupClick = { /* T-detalhe de grupo (RF42) ainda não existe. */ },
        )
    }
}
