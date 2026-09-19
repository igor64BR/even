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
import com.rateio.app.ui.creategroup.CreateGroupRoute
import com.rateio.app.ui.creategroup.CreateGroupViewModelFactory
import com.rateio.app.ui.createexpense.CreateExpenseRoute
import com.rateio.app.ui.createexpense.CreateExpenseViewModelFactory
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
 * Destinos navegáveis a partir da raiz. Sem NavHost ainda (T9, bottom nav de verdade, não existe)
 * — essa é uma máquina de estados mínima entre as telas que já existem.
 *
 * [CreateExpense] (T24) carrega [CreateExpense.groupId] porque não existe tela de detalhe de
 * grupo ainda (RF42) — tocar num [com.rateio.app.ui.groups.GroupCard] entra direto em "Nova
 * despesa" daquele grupo, mesmo precedente que T19 documentou pra "Sincronizar este grupo" (ação
 * embutida no card em vez de uma tela dedicada que ainda não existe).
 */
private sealed interface RateioDestination {
    data object GroupList : RateioDestination
    data object CreateGroup : RateioDestination
    data class CreateExpense(val groupId: String) : RateioDestination
    data object Login : RateioDestination
}

@Composable
private fun RateioApp(container: AppContainer) {
    var destination by remember { mutableStateOf<RateioDestination>(RateioDestination.GroupList) }

    val groupListViewModelFactory = GroupListViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
        expenseRepository = container.expenseRepository,
        authRepository = container.authRepository,
        remoteGroupRepository = container.remoteGroupRepository,
    )
    val createGroupViewModelFactory = CreateGroupViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )
    val authViewModelFactory = AuthViewModelFactory(
        authRepository = container.authRepository,
        googleIdentityClient = container.googleIdentityClient,
    )

    RateioTheme {
        when (val current = destination) {
            RateioDestination.GroupList -> GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { destination = RateioDestination.CreateGroup },
                onGroupClick = { groupId -> destination = RateioDestination.CreateExpense(groupId) },
                onProfileClick = { destination = RateioDestination.Login },
            )

            RateioDestination.CreateGroup -> CreateGroupRoute(
                factory = createGroupViewModelFactory,
                onGroupCreated = { destination = RateioDestination.GroupList },
                onBackClick = { destination = RateioDestination.GroupList },
            )

            is RateioDestination.CreateExpense -> CreateExpenseRoute(
                factory = CreateExpenseViewModelFactory(
                    groupId = current.groupId,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                ),
                onExpenseCreated = { destination = RateioDestination.GroupList },
                onBackClick = { destination = RateioDestination.GroupList },
            )

            RateioDestination.Login -> LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { destination = RateioDestination.GroupList },
                onContinueWithoutAccount = { destination = RateioDestination.GroupList },
            )
        }
    }
}
