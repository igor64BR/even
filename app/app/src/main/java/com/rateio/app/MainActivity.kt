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
import com.rateio.app.ui.groupdetail.GroupDetailRoute
import com.rateio.app.ui.groupdetail.GroupDetailViewModelFactory
import com.rateio.app.ui.groups.GroupListRoute
import com.rateio.app.ui.groups.GroupListViewModelFactory
import com.rateio.app.ui.settledebts.SettleDebtsRoute
import com.rateio.app.ui.settledebts.SettleDebtsViewModelFactory
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
 * [GroupDetail] (T42.2/T42.4, RF42) é o destino real de tocar num
 * [com.rateio.app.ui.groups.GroupCard] — até T42.4 isso ia direto pra [CreateExpense] ou
 * disparava "Sincronizar" no próprio card, atalhos temporários documentados por T19/T24 porque
 * esta tela não existia ainda. [CreateExpense] e [SettleDebts] continuam existindo, só que agora
 * são alcançados a partir de [GroupDetail], não direto da lista.
 */
private sealed interface RateioDestination {
    data object GroupList : RateioDestination
    data object CreateGroup : RateioDestination
    data class GroupDetail(val groupId: String) : RateioDestination
    data class CreateExpense(val groupId: String) : RateioDestination
    data class SettleDebts(val groupId: String) : RateioDestination
    data object Login : RateioDestination
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
    val authViewModelFactory = AuthViewModelFactory(
        authRepository = container.authRepository,
        googleIdentityClient = container.googleIdentityClient,
    )

    RateioTheme {
        when (val current = destination) {
            RateioDestination.GroupList -> GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { destination = RateioDestination.CreateGroup },
                onGroupClick = { groupId -> destination = RateioDestination.GroupDetail(groupId) },
                onProfileClick = { destination = RateioDestination.Login },
            )

            RateioDestination.CreateGroup -> CreateGroupRoute(
                factory = createGroupViewModelFactory,
                onGroupCreated = { destination = RateioDestination.GroupList },
                onBackClick = { destination = RateioDestination.GroupList },
            )

            is RateioDestination.GroupDetail -> GroupDetailRoute(
                factory = GroupDetailViewModelFactory(
                    groupId = current.groupId,
                    groupRepository = container.groupRepository,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    settlementRepository = container.settlementRepository,
                    authRepository = container.authRepository,
                    remoteGroupRepository = container.remoteGroupRepository,
                    debtSimplificationEngine = container.debtSimplificationEngine,
                ),
                onBackClick = { destination = RateioDestination.GroupList },
                onCreateExpenseClick = { destination = RateioDestination.CreateExpense(current.groupId) },
                onSettleDebtsClick = { destination = RateioDestination.SettleDebts(current.groupId) },
            )

            is RateioDestination.CreateExpense -> CreateExpenseRoute(
                factory = CreateExpenseViewModelFactory(
                    groupId = current.groupId,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                ),
                onExpenseCreated = { destination = RateioDestination.GroupDetail(current.groupId) },
                onBackClick = { destination = RateioDestination.GroupDetail(current.groupId) },
            )

            is RateioDestination.SettleDebts -> SettleDebtsRoute(
                factory = SettleDebtsViewModelFactory(
                    groupId = current.groupId,
                    groupRepository = container.groupRepository,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    settlementRepository = container.settlementRepository,
                    debtSimplificationEngine = container.debtSimplificationEngine,
                ),
                onBackClick = { destination = RateioDestination.GroupDetail(current.groupId) },
            )

            RateioDestination.Login -> LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { destination = RateioDestination.GroupList },
                onContinueWithoutAccount = { destination = RateioDestination.GroupList },
            )
        }
    }
}
