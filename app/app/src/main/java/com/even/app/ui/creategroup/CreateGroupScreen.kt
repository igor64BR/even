package com.even.app.ui.creategroup

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
import com.even.app.ui.groups.EvenBottomBar
import com.even.app.ui.groups.EvenBottomTab
import com.even.app.ui.theme.LocalEvenColors

/**
 * The "New group" screen, opened from "Your groups"'s FAB
 * ([com.even.app.ui.groups.CreateGroupFab]). `factory` injects the [CreateGroupViewModel]
 * through [com.even.app.di.AppContainer]'s manual composition — the same pattern as
 * [com.even.app.ui.groups.GroupListRoute].
 *
 * [key] identifies this visit to the screen for Compose's `viewModel()` (see the KDoc of
 * `EvenDestination.CreateGroup.instanceId` in `MainActivity`) — without it, reopening "New group"
 * would reuse the previous visit's `ViewModel`, with the form and `isSaving` from the last
 * submission still stuck.
 */
@Composable
fun CreateGroupRoute(
    factory: CreateGroupViewModelFactory,
    onGroupCreated: () -> Unit,
    onBackClick: () -> Unit,
    key: String? = null,
    unreadNotificationsCount: Int = 0,
    onGroupsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    authenticatedUserName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateGroupViewModel = viewModel(factory = factory, key = key)
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
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

/**
 * A thin Composable: only orchestrates the topbar + fields + button — none of them know where the
 * state comes from or do validation/persistence (same Object Calisthenics as
 * [com.even.app.ui.groups.GroupListScreen]).
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
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalEvenColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            CreateGroupTopBar(onBackClick = onBackClick, isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        },
        bottomBar = {
            EvenBottomBar(
                selectedTab = EvenBottomTab.GROUPS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        EvenBottomTab.GROUPS -> onGroupsClick()
                        EvenBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        EvenBottomTab.PROFILE -> onProfileClick()
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
                    containerColor = colors.brand,
                    contentColor = colors.onBrand,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(text = "Create group", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
