package com.rateio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rateio.app.ui.joingroup.JoinGroupRoute
import com.rateio.app.ui.joingroup.JoinGroupViewModelFactory
import com.rateio.app.ui.settledebts.SettleDebtsRoute
import com.rateio.app.ui.settledebts.SettleDebtsViewModelFactory
import com.rateio.app.ui.theme.RateioTheme

/**
 * Ponto de entrada do app (RF40 — abre direto em "Seus grupos", sem login). Só monta o tema e
 * delega pra [GroupListRoute]; nenhuma lógica de UI mora aqui.
 *
 * [pendingInviteCode] (T22.1) é a única lógica de plataforma que precisa viver na Activity em vez
 * de num ViewModel: extrair o código de convite de um [Intent] (deep link
 * `rateio://join/{codigo}`, `AndroidManifest.xml`) exige `Intent`/`Uri`, que não fazem sentido
 * vazar pra `:domain`/ViewModels. `android:launchMode="singleTop"` garante que reabrir o link com
 * o app já em memória chega em [onNewIntent] em vez de recriar a Activity.
 */
class MainActivity : ComponentActivity() {

    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingInviteCode = intent.extractInviteCode()

        val container = (application as RateioApplication).container
        setContent {
            RateioApp(
                container = container,
                pendingInviteCode = pendingInviteCode,
                onPendingInviteCodeConsumed = { pendingInviteCode = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingInviteCode = intent.extractInviteCode()
    }
}

/**
 * `rateio://join/{codigo}` — scheme e host fixos do deep link de T22.1, código do convite no
 * primeiro segmento do path. `null` pra qualquer intent que não seja esse link (abertura normal
 * pelo launcher, outras actions). `internal` (em vez de `private`) só pra ser testável direto —
 * mesma convenção de [com.rateio.app.ui.creategroup.FieldLabel].
 */
internal fun Intent?.extractInviteCode(): String? {
    val uri = this?.data ?: return null
    if (action != Intent.ACTION_VIEW) return null
    if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) return null
    return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
}

private const val DEEP_LINK_SCHEME = "rateio"
private const val DEEP_LINK_HOST = "join"

/**
 * Destinos navegáveis a partir da raiz. Sem NavHost ainda (T9, bottom nav de verdade, não existe)
 * — essa é uma máquina de estados mínima entre as telas que já existem.
 *
 * [GroupDetail] (T42.2/T42.4, RF42) é o destino real de tocar num
 * [com.rateio.app.ui.groups.GroupCard] — até T42.4 isso ia direto pra [CreateExpense] ou
 * disparava "Sincronizar" no próprio card, atalhos temporários documentados por T19/T24 porque
 * esta tela não existia ainda. [CreateExpense] e [SettleDebts] continuam existindo, só que agora
 * são alcançados a partir de [GroupDetail], não direto da lista.
 *
 * [Login.pendingInviteCode] (T22.1) carrega a intenção de entrar num grupo quando o deep link
 * chega com o usuário deslogado — `null` no acesso normal (ícone de perfil). [JoinGroup] é o
 * destino da tela de confirmação (T22.2), alcançado direto do deep link (usuário logado) ou como
 * retomada depois de [Login] (usuário logava primeiro).
 */
private sealed interface RateioDestination {
    data object GroupList : RateioDestination
    data object CreateGroup : RateioDestination
    data class GroupDetail(val groupId: String) : RateioDestination
    data class CreateExpense(val groupId: String) : RateioDestination
    data class SettleDebts(val groupId: String) : RateioDestination
    data class Login(val pendingInviteCode: String? = null) : RateioDestination
    data class JoinGroup(val inviteCode: String) : RateioDestination
}

@Composable
private fun RateioApp(
    container: AppContainer,
    pendingInviteCode: String? = null,
    onPendingInviteCodeConsumed: () -> Unit = {},
) {
    var destination by remember { mutableStateOf<RateioDestination>(RateioDestination.GroupList) }

    // T22.1 — deep link `rateio://join/{codigo}` (extraído do Intent em MainActivity). Roteia
    // direto pra JoinGroup independente de sessão: é o próprio JoinGroupViewModel que checa login
    // e expõe NeedsLogin — RateioApp só reage a esse estado (abaixo, no case JoinGroup) mandando
    // pra Login com o código guardado, sem duplicar a checagem de sessão aqui.
    LaunchedEffect(pendingInviteCode) {
        val code = pendingInviteCode ?: return@LaunchedEffect
        destination = RateioDestination.JoinGroup(code)
        onPendingInviteCodeConsumed()
    }

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
                onProfileClick = { destination = RateioDestination.Login() },
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

            is RateioDestination.Login -> LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { destination = RateioDestination.GroupList },
                onContinueWithoutAccount = { destination = RateioDestination.GroupList },
                onSignedIn = current.pendingInviteCode?.let { code ->
                    { destination = RateioDestination.JoinGroup(code) }
                },
            )

            is RateioDestination.JoinGroup -> JoinGroupRoute(
                factory = JoinGroupViewModelFactory(
                    inviteCode = current.inviteCode,
                    authRepository = container.authRepository,
                    remoteGroupRepository = container.remoteGroupRepository,
                ),
                onNeedsLogin = { code -> destination = RateioDestination.Login(pendingInviteCode = code) },
                onDone = { destination = RateioDestination.GroupList },
            )
        }
    }
}
