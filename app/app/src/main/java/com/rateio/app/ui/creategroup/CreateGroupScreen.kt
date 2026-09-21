package com.rateio.app.ui.creategroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.groups.RateioBottomBar
import com.rateio.app.ui.groups.RateioBottomTab
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * Tela "Novo grupo" (T16), aberta a partir do FAB de "Seus grupos" (T8/[com.rateio.app.ui.groups.CreateGroupFab]).
 * `factory` injeta o [CreateGroupViewModel] pela composição manual de
 * [com.rateio.app.di.AppContainer] — mesmo padrão de
 * [com.rateio.app.ui.groups.GroupListRoute].
 */
@Composable
fun CreateGroupRoute(
    factory: CreateGroupViewModelFactory,
    onGroupCreated: () -> Unit,
    onBackClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateGroupViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateGroupEvent.GroupCreated -> onGroupCreated()
            }
        }
    }

    CreateGroupScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onNewParticipantNameChange = viewModel::onNewParticipantNameChanged,
        onAddParticipant = viewModel::onAddParticipant,
        onRemoveParticipant = viewModel::onRemoveParticipant,
        onSaveClick = viewModel::onSaveClick,
        unreadNotificationsCount = unreadNotificationsCount,
        onGroupsClick = onGroupsClick,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        authenticatedUserName = authenticatedUserName,
        modifier = modifier,
    )
}

/**
 * Composable fina: só orquestra topbar + campos + botão — nenhuma sabe de onde vem o estado
 * nem faz validação/persistência (mesmo Object Calisthenics de
 * [com.rateio.app.ui.groups.GroupListScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategorySelected: (GroupCategory) -> Unit,
    onNewParticipantNameChange: (String) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onSaveClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { CreateGroupTopBar(onBackClick = onBackClick) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GroupNameField(
                name = uiState.name,
                isError = uiState.nameError,
                onNameChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            CategoryChipRow(
                selectedCategory = uiState.category,
                onCategorySelected = onCategorySelected,
            )
            ParticipantsField(
                participants = uiState.participants,
                newParticipantName = uiState.newParticipantName,
                isError = uiState.participantsError,
                onNewParticipantNameChange = onNewParticipantNameChange,
                onAddParticipant = onAddParticipant,
                onRemoveParticipant = onRemoveParticipant,
            )
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandInk,
                    contentColor = colors.onBrand,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(text = "Criar grupo", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
