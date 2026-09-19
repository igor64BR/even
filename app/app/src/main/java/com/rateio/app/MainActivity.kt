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
import com.rateio.app.ui.creategroup.CreateGroupRoute
import com.rateio.app.ui.creategroup.CreateGroupViewModelFactory
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

/** Destinos navegáveis a partir da raiz. Sem NavHost ainda — só duas telas, estado local basta. */
private sealed interface RateioDestination {
    data object GroupList : RateioDestination
    data object CreateGroup : RateioDestination
}

@Composable
private fun RateioApp(container: AppContainer) {
    var destination by remember { mutableStateOf<RateioDestination>(RateioDestination.GroupList) }

    val groupListViewModelFactory = GroupListViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )
    val createGroupViewModelFactory = CreateGroupViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )

    RateioTheme {
        when (destination) {
            RateioDestination.GroupList -> GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { destination = RateioDestination.CreateGroup },
                onGroupClick = { /* T-detalhe de grupo (RF42) ainda não existe. */ },
            )

            RateioDestination.CreateGroup -> CreateGroupRoute(
                factory = createGroupViewModelFactory,
                onGroupCreated = { destination = RateioDestination.GroupList },
                onBackClick = { destination = RateioDestination.GroupList },
            )
        }
    }
}
