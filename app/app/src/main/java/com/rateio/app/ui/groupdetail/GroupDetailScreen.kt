package com.rateio.app.ui.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.groups.RateioBottomBar
import com.rateio.app.ui.groups.RateioBottomTab
import com.rateio.app.ui.groups.SyncStatusIcon
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * Tela "Detalhes do grupo" (T42.2/RF42), aberta a partir do
 * [com.rateio.app.ui.groups.GroupCard] — até T42.4 tocar num card ia direto pra "Nova despesa"
 * (T24) ou "Sincronizar" (T19), atalhos temporários porque esta tela não existia. `factory` injeta
 * o [GroupDetailViewModel] pela composição manual de [com.rateio.app.di.AppContainer], mesmo
 * padrão de [com.rateio.app.ui.createexpense.CreateExpenseRoute].
 *
 * `DisposableEffect` liga o cliente SignalR (T40.1) à presença desta tela na composição — conecta
 * ao entrar, desconecta ao sair, independente do ciclo de vida do `ViewModel` em si (que hoje pode
 * sobreviver à navegação, já que o app ainda não usa `NavHost`/back stack real). É essa
 * `DisposableEffect`, não o `ViewModel`, que garante "só conecta enquanto a tela está sendo
 * vista" (constitution.md princípio 3).
 */
@Composable
fun GroupDetailRoute(
    factory: GroupDetailViewModelFactory,
    onBackClick: () -> Unit,
    onCreateExpenseClick: () -> Unit,
    onEditExpenseClick: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: GroupDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        viewModel.startRealtimeUpdates()
        onDispose { viewModel.stopRealtimeUpdates() }
    }

    GroupDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onCreateExpenseClick = onCreateExpenseClick,
        onEditExpenseClick = onEditExpenseClick,
        onDeleteExpenseConfirmed = viewModel::onDeleteExpenseClick,
        onSettleDebtsClick = onSettleDebtsClick,
        onSyncClick = viewModel::onSyncGroupClick,
        unreadNotificationsCount = unreadNotificationsCount,
        onGroupsClick = onGroupsClick,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        authenticatedUserName = authenticatedUserName,
        modifier = modifier,
    )
}

/**
 * Composable fina: título/estado vem de [uiState], corpo delega pra
 * [GroupDetailContent]/estados de carregamento — nenhuma delas calcula saldo/divisão (Object
 * Calisthenics, mesmo padrão de [com.rateio.app.ui.groups.GroupListScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    uiState: GroupDetailUiState,
    onBackClick: () -> Unit,
    onCreateExpenseClick: () -> Unit,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    onSyncClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    val title = (uiState as? GroupDetailUiState.Content)?.groupName ?: "Grupo"

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { GroupDetailTopBar(groupName = title, onBackClick = onBackClick) },
        floatingActionButton = {
            if (uiState is GroupDetailUiState.Content) {
                FloatingActionButton(
                    onClick = onCreateExpenseClick,
                    containerColor = colors.brandInk,
                    contentColor = colors.onBrand,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Nova despesa")
                }
            }
        },
        bottomBar = {
            RateioBottomBar(
                selectedTab = RateioBottomTab.GRUPOS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        RateioBottomTab.GRUPOS -> onGroupsClick()
                        RateioBottomTab.AVISOS -> onNotificationsClick()
                        RateioBottomTab.PERFIL -> onProfileClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is GroupDetailUiState.Loading -> Unit
                is GroupDetailUiState.NotFound -> GroupNotFoundState()
                is GroupDetailUiState.Content -> GroupDetailContent(
                    uiState = uiState,
                    onEditExpenseClick = onEditExpenseClick,
                    onDeleteExpenseConfirmed = onDeleteExpenseConfirmed,
                    onSettleDebtsClick = onSettleDebtsClick,
                    onSyncClick = onSyncClick,
                )
            }
        }
    }
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailUiState.Content,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    onSettleDebtsClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    val colors = LocalRateioColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        GroupEyebrow(participantCount = uiState.participantCount, isSynced = uiState.isSynced)

        SectionLabel(text = "Saldos")
        Column {
            uiState.balances.forEach { participant -> ParticipantBalanceRow(participant = participant) }
        }

        Button(
            onClick = onSettleDebtsClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brandInk, contentColor = colors.onBrand),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(text = "Quitar dívidas", fontWeight = FontWeight.SemiBold)
        }

        SyncSection(syncAction = uiState.syncAction, onSyncClick = onSyncClick)

        SectionLabel(text = "Despesas")
        if (uiState.expenses.isEmpty()) {
            EmptyExpensesState()
        } else {
            ExpenseList(
                expenses = uiState.expenses,
                onEditExpenseClick = onEditExpenseClick,
                onDeleteExpenseConfirmed = onDeleteExpenseConfirmed,
            )
        }

        if (uiState.settlements.isNotEmpty()) {
            SectionLabel(text = "Histórico de quitações")
            Column {
                uiState.settlements.forEach { settlement -> SettlementRow(settlement = settlement) }
            }
        }
    }
}

/**
 * T29.2: [ExpenseDeleteConfirmationState] guarda qual despesa está com exclusão pendente — o
 * primeiro toque no ícone de lixeira de [ExpenseRow] só chega até [ExpenseDeleteConfirmationState
 * .request] (abre o diálogo), nunca exclui direto; só "Excluir" no [AlertDialog] chama
 * [ExpenseDeleteConfirmationState.confirm], que aí sim dispara [onDeleteExpenseConfirmed]. Estado
 * de UI pura (não sobrevive rotação/processo morto, sem necessidade — reabrir a confirmação é
 * barato), por isso `remember` em vez de morar no `ViewModel`.
 */
@Composable
private fun ExpenseList(
    expenses: List<ExpenseRowUiModel>,
    onEditExpenseClick: (String) -> Unit,
    onDeleteExpenseConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteConfirmation = remember { ExpenseDeleteConfirmationState() }

    Column(modifier = modifier) {
        expenses.forEach { expense ->
            ExpenseRow(
                expense = expense,
                onClick = { onEditExpenseClick(expense.id) },
                onDeleteClick = { deleteConfirmation.request(expense.id) },
            )
        }
    }

    if (deleteConfirmation.pendingExpenseId != null) {
        DeleteExpenseConfirmationDialog(
            onConfirm = { deleteConfirmation.confirm(onConfirmed = onDeleteExpenseConfirmed) },
            onDismiss = deleteConfirmation::dismiss,
        )
    }
}

@Composable
private fun DeleteExpenseConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalRateioColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Excluir despesa?", fontWeight = FontWeight.Bold) },
        text = { Text(text = "Essa despesa some da lista e o saldo do grupo é recalculado. Não dá pra desfazer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Excluir", color = colors.danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        },
    )
}

@Composable
private fun GroupEyebrow(participantCount: Int, isSynced: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(top = 4.dp),
    ) {
        Text(
            text = "$participantCount ${if (participantCount == 1) "pessoa" else "pessoas"}",
            fontSize = 11.sp,
            color = colors.inkSoft,
        )
        SyncStatusIcon(isSynced = isSynced)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        color = colors.inkSoft,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

/**
 * "Sincronizar este grupo" (T19), movida do card da lista pra cá em T42.4. Só desenha algo
 * quando há uma ação disponível — grupo já sincronizado ou usuário deslogado não ganham espaço
 * extra na tela (mesma regra de [com.rateio.app.ui.groupdetail.GroupSyncActionUiState.Hidden]).
 */
@Composable
private fun SyncSection(syncAction: GroupSyncActionUiState, onSyncClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    when (syncAction) {
        GroupSyncActionUiState.Hidden -> Unit

        GroupSyncActionUiState.Available -> Button(
            onClick = onSyncClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.paperAlt, contentColor = colors.ink),
            modifier = modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            Text(text = "Sincronizar este grupo", fontWeight = FontWeight.SemiBold)
        }

        GroupSyncActionUiState.InProgress -> Text(
            text = "Sincronizando…",
            color = colors.inkSoft,
            fontSize = 13.sp,
            modifier = modifier.padding(top = 10.dp),
        )

        is GroupSyncActionUiState.Failed -> Text(
            text = "${syncAction.message} — tocar pra tentar de novo",
            color = colors.danger,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = modifier.padding(top = 10.dp).clickable(onClick = onSyncClick),
        )
    }
}

@Composable
private fun GroupNotFoundState(modifier: Modifier = Modifier) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier.fillMaxWidth().padding(top = 60.dp, start = 20.dp, end = 20.dp)) {
        Text(text = "Grupo não encontrado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.ink)
    }
}
