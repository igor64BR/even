package com.rateio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rateio.app.di.AppContainer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import com.rateio.app.ui.notifications.NotificationsRoute
import com.rateio.app.ui.notifications.NotificationsViewModelFactory
import com.rateio.app.ui.settledebts.SettleDebtsRoute
import com.rateio.app.ui.settledebts.SettleDebtsViewModelFactory
import com.rateio.app.ui.theme.RateioTheme
import java.util.UUID

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
 *
 * [Notifications] (T41.1, RF35/RF36) é o destino da aba "Avisos" da bottom nav —
 * `RateioBottomBar` está presente em toda tela alcançável a partir da raiz (mesmo padrão do
 * protótipo: `grupo.html`/`nova-despesa.html`/`quitar.html`/`criar-grupo.html`/`login.html`
 * sempre têm `.bottombar`, com "Grupos" marcada ativa nas telas que são sub-fluxo da lista de
 * grupos e "Perfil" ativa em [Login]), não só em [GroupList]/[Notifications].
 *
 * [CreateExpense.expenseId] (T29) é `null` pra "Nova despesa" (alcançada pelo FAB de
 * [GroupDetail]) e o id da despesa sendo editada quando vem de [GroupDetail]'s
 * `onEditExpenseClick` (tocar numa [com.rateio.app.ui.groupdetail.ExpenseRow]) — mesmo destino
 * pros dois fluxos, só muda o parâmetro (T29, "edição é estado, não tela nova").
 *
 * [CreateGroup.instanceId]/[CreateExpense.instanceId]: sem `NavHost`, todo destino compartilha o
 * mesmo `ViewModelStoreOwner` (a própria Activity) — `viewModel(factory=...)` sem uma `key`
 * explícita cacheia por classe, não por navegação, então reabrir "Novo grupo"/"Nova despesa"
 * devolvia o `ViewModel` da visita anterior, com o formulário inteiro (e `isSaving`) ainda no
 * estado da última submissão. `instanceId` gera um valor novo a cada `RateioDestination.CreateGroup()`/
 * `CreateExpense()` construído (default de `UUID.randomUUID()`), viram a `key` desse `viewModel()`
 * (ver `RateioApp`) e garantem um formulário zerado a cada visita. [GroupDetail]/[SettleDebts] não
 * precisam disso — o `ViewModel` dos dois é só um espelho de `Flow`s do Room (sem "isSaving" nem
 * campo de formulário pra ficar velho), então a `key` ali é só o `groupId`: revisitar o MESMO
 * grupo reaproveita a instância (barato, inofensivo), visitar um grupo DIFERENTE já força uma
 * nova (que era o bug real ali — sem isso, o segundo grupo aberto mostraria os dados do primeiro).
 */
private sealed interface RateioDestination {
    data object GroupList : RateioDestination
    data class CreateGroup(val instanceId: String = UUID.randomUUID().toString()) : RateioDestination
    data class GroupDetail(val groupId: String) : RateioDestination
    data class CreateExpense(
        val groupId: String,
        val expenseId: String? = null,
        val instanceId: String = UUID.randomUUID().toString(),
    ) : RateioDestination
    data class SettleDebts(val groupId: String) : RateioDestination
    data class Login(val pendingInviteCode: String? = null) : RateioDestination
    data class JoinGroup(val inviteCode: String) : RateioDestination
    data object Notifications : RateioDestination
}

@Composable
private fun RateioApp(
    container: AppContainer,
    pendingInviteCode: String? = null,
    onPendingInviteCodeConsumed: () -> Unit = {},
) {
    var destination by remember { mutableStateOf<RateioDestination>(RateioDestination.GroupList) }

    // Badge de não lidas (T41.2) hoisted aqui em vez de injetado em cada ViewModel: é a única
    // peça de estado que toda tela por trás de RateioBottomBar precisa, e nenhuma delas (edição de
    // despesa, criar grupo, login, ...) tem qualquer outro motivo pra conhecer
    // NotificationRepository — adicionar essa dependência a cada uma só pra pintar um badge violaria
    // a responsabilidade única de cada ViewModel. GroupList/Notifications continuam com sua própria
    // fonte (já existia antes desta tela ganhar `RateioBottomBar` em todo lugar).
    val unreadNotificationsCount by container.notificationRepository.getUnreadCountFlow()
        .collectAsStateWithLifecycle(initialValue = 0)

    // Nome do usuário autenticado (ou `null` deslogado) hoisted pelo mesmo motivo do badge acima —
    // troca o rótulo "Entrar"/"Perfil" e o ícone (genérico/avatar com iniciais) de `RateioBottomBar`
    // em toda tela, espelhando `renderHeaderAuth` do protótipo (`prototype/app.js`).
    val authenticatedUserName by container.authRepository.getSessionFlow()
        .map { session -> session?.user?.name }
        .collectAsStateWithLifecycle(initialValue = null)

    // Botão sol/lua (mesmo componente global de `prototype/app.js`'s `initThemeToggle`, presente
    // no TopAppBar de toda tela). `storedThemePreference` é `null` até o usuário tocar no botão
    // pela primeira vez — nesse caso o app segue o tema do sistema, igual o protótipo seguia
    // `prefers-color-scheme` antes de qualquer escolha salva em `localStorage`. Depois do primeiro
    // toque, o valor salvo manda, em todo lançamento futuro, até o usuário tocar de novo.
    val storedThemePreference by container.themeRepository.getIsDarkThemeFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val isDarkTheme = storedThemePreference ?: isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val onToggleTheme: () -> Unit = {
        coroutineScope.launch { container.themeRepository.setDarkTheme(!isDarkTheme) }
    }

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
        notificationRepository = container.notificationRepository,
    )
    val notificationsViewModelFactory = NotificationsViewModelFactory(
        notificationRepository = container.notificationRepository,
        authRepository = container.authRepository,
    )
    val createGroupViewModelFactory = CreateGroupViewModelFactory(
        groupRepository = container.groupRepository,
        participantRepository = container.participantRepository,
    )
    val authViewModelFactory = AuthViewModelFactory(
        authRepository = container.authRepository,
        googleIdentityClient = container.googleIdentityClient,
    )

    RateioTheme(darkTheme = isDarkTheme) {
        when (val current = destination) {
            RateioDestination.GroupList -> GroupListRoute(
                factory = groupListViewModelFactory,
                onCreateGroupClick = { destination = RateioDestination.CreateGroup() },
                onGroupClick = { groupId -> destination = RateioDestination.GroupDetail(groupId) },
                onProfileClick = { destination = RateioDestination.Login() },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )

            is RateioDestination.CreateGroup -> CreateGroupRoute(
                factory = createGroupViewModelFactory,
                key = "CreateGroup:${current.instanceId}",
                onGroupCreated = { destination = RateioDestination.GroupList },
                onBackClick = { destination = RateioDestination.GroupList },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                onProfileClick = { destination = RateioDestination.Login() },
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
                    remoteExpenseRepository = container.remoteExpenseRepository,
                    debtSimplificationEngine = container.debtSimplificationEngine,
                    groupRealtimeGateway = container.groupRealtimeGateway,
                ),
                key = "GroupDetail:${current.groupId}",
                onBackClick = { destination = RateioDestination.GroupList },
                onCreateExpenseClick = { destination = RateioDestination.CreateExpense(current.groupId) },
                onEditExpenseClick = { expenseId ->
                    destination = RateioDestination.CreateExpense(current.groupId, expenseId)
                },
                onSettleDebtsClick = { destination = RateioDestination.SettleDebts(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                onProfileClick = { destination = RateioDestination.Login() },
            )

            is RateioDestination.CreateExpense -> CreateExpenseRoute(
                factory = CreateExpenseViewModelFactory(
                    groupId = current.groupId,
                    expenseId = current.expenseId,
                    participantRepository = container.participantRepository,
                    expenseRepository = container.expenseRepository,
                    groupRepository = container.groupRepository,
                    remoteExpenseRepository = container.remoteExpenseRepository,
                ),
                key = "CreateExpense:${current.instanceId}",
                onSaved = { destination = RateioDestination.GroupDetail(current.groupId) },
                onBackClick = { destination = RateioDestination.GroupDetail(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                onProfileClick = { destination = RateioDestination.Login() },
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
                key = "SettleDebts:${current.groupId}",
                onBackClick = { destination = RateioDestination.GroupDetail(current.groupId) },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                onProfileClick = { destination = RateioDestination.Login() },
            )

            is RateioDestination.Login -> LoginRoute(
                factory = authViewModelFactory,
                onBackClick = { destination = RateioDestination.GroupList },
                onContinueWithoutAccount = { destination = RateioDestination.GroupList },
                onSignedIn = current.pendingInviteCode?.let { code ->
                    { destination = RateioDestination.JoinGroup(code) }
                },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
            )

            is RateioDestination.JoinGroup -> JoinGroupRoute(
                factory = JoinGroupViewModelFactory(
                    inviteCode = current.inviteCode,
                    authRepository = container.authRepository,
                    remoteGroupRepository = container.remoteGroupRepository,
                ),
                key = "JoinGroup:${current.inviteCode}",
                onNeedsLogin = { code -> destination = RateioDestination.Login(pendingInviteCode = code) },
                onDone = { destination = RateioDestination.GroupList },
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onNotificationsClick = { destination = RateioDestination.Notifications },
                onProfileClick = { destination = RateioDestination.Login() },
            )

            RateioDestination.Notifications -> NotificationsRoute(
                factory = notificationsViewModelFactory,
                onGroupsClick = { destination = RateioDestination.GroupList },
                onProfileClick = { destination = RateioDestination.Login() },
                authenticatedUserName = authenticatedUserName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        }
    }
}
