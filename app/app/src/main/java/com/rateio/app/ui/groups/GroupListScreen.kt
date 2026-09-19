package com.rateio.app.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * Tela "Seus grupos" (RF40/RF41), ponto de entrada do app. `factory` injeta o
 * [GroupListViewModel] pela composição manual de [com.rateio.app.di.AppContainer] (sem
 * framework de DI ainda) — a Composable em si não sabe de onde vem o estado.
 */
@Composable
fun GroupListRoute(
    factory: GroupListViewModelFactory,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: GroupListViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupListScreen(
        uiState = uiState,
        onCreateGroupClick = onCreateGroupClick,
        onGroupClick = onGroupClick,
        onSyncGroupClick = viewModel::onSyncGroupClick,
        onProfileClick = onProfileClick,
        modifier = modifier,
    )
}

/**
 * Composable fina: só orquestra [GroupListTopBar], corpo (lista/vazio/carregando),
 * [CreateGroupFab] e [RateioBottomBar] — nenhuma delas sabe de onde vem o estado (Object
 * Calisthenics: uma responsabilidade por Composable, nomeada pelo que apresenta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    uiState: GroupListUiState,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onSyncGroupClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    var selectedTab by remember { mutableStateOf(RateioBottomTab.GRUPOS) }

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { GroupListTopBar() },
        floatingActionButton = { CreateGroupFab(onClick = onCreateGroupClick) },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            RateioBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    // T12 — aba "Perfil" é o equivalente ao auth-slot do protótipo
                    // (ver prototype/app.js, renderHeaderAuth): entrada pra tela de login/conta.
                    if (tab == RateioBottomTab.PERFIL) onProfileClick()
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GroupListBody(
                uiState = uiState,
                onCreateGroupClick = onCreateGroupClick,
                onGroupClick = onGroupClick,
                onSyncGroupClick = onSyncGroupClick,
            )
        }
    }
}

/**
 * Estado de carregamento não tem tratamento visual próprio no protótipo (é só localStorage
 * síncrono lá); aqui, enquanto o primeiro valor do Room não chega, a tela fica em branco por um
 * instante — aceitável para esta task, um spinner dedicado fica para quando houver necessidade
 * real (ex.: sincronização, que é assíncrona de verdade).
 */
@Composable
private fun GroupListBody(
    uiState: GroupListUiState,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onSyncGroupClick: (String) -> Unit,
) {
    when (uiState) {
        is GroupListUiState.Loading -> Unit
        is GroupListUiState.Empty -> EmptyGroupsState(onCreateGroupClick = onCreateGroupClick)
        is GroupListUiState.Content -> GroupList(
            groups = uiState.groups,
            onGroupClick = onGroupClick,
            onSyncGroupClick = onSyncGroupClick,
        )
    }
}

@Composable
private fun GroupList(
    groups: List<GroupListItemUiModel>,
    onGroupClick: (String) -> Unit,
    onSyncGroupClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(items = groups, key = { it.id }) { group ->
            GroupCard(group = group, onClick = onGroupClick, onSyncClick = onSyncGroupClick)
        }
    }
}
