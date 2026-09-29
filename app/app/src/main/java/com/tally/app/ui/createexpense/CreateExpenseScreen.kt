package com.tally.app.ui.createexpense

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
import com.tally.app.ui.format.parseAmountInputToCents
import com.tally.app.ui.groups.TallyBottomBar
import com.tally.app.ui.groups.TallyBottomTab
import com.tally.app.ui.theme.LocalTallyColors
import com.tally.domain.model.Money
import java.time.LocalDate

/**
 * The "New expense" screen (T24), opened from the "+" button in "Group details" (T42.2/RF42) —
 * until T42.4 it opened directly from [com.tally.app.ui.groups.GroupCard] in the group list, a
 * temporary shortcut because the detail screen didn't exist yet. `factory` injects the
 * [CreateExpenseViewModel] through [com.tally.app.di.AppContainer]'s manual composition, the same
 * pattern as [com.tally.app.ui.creategroup.CreateGroupRoute]. A non-null `factory.expenseId`
 * (T29) is the only trigger for edit mode — neither this Route nor [CreateExpenseScreen] decide
 * that, they just pass along [CreateExpenseUiState.isEditMode] to paint the title/button.
 *
 * [key] identifies this visit to the screen for Compose's `viewModel()` (see the KDoc of
 * `TallyDestination.CreateExpense.instanceId` in `MainActivity`) — without it, logging a second
 * expense right after the first would reuse the first one's `ViewModel`, with the form and
 * `isSaving` from the last submission still stuck.
 */
@Composable
fun CreateExpenseRoute(
    factory: CreateExpenseViewModelFactory,
    onSaved: () -> Unit,
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
    val viewModel: CreateExpenseViewModel = viewModel(factory = factory, key = key)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateExpenseEvent.Saved -> onSaved()
            }
        }
    }

    CreateExpenseScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onDescriptionChange = viewModel::onDescriptionChanged,
        onAmountChange = viewModel::onAmountChanged,
        onPayerSelected = viewModel::onPayerSelected,
        onDateSelected = viewModel::onDateSelected,
        onSplitModeSelected = viewModel::onSplitModeSelected,
        onParticipantToggled = viewModel::onParticipantToggled,
        onPercentageChanged = viewModel::onPercentageChanged,
        onFixedAmountChanged = viewModel::onFixedAmountChanged,
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
 * A thin Composable: only orchestrates the topbar + fields + participant list + button — none of
 * them know where the state comes from or do validation/calculation (same Object Calisthenics as
 * [com.tally.app.ui.creategroup.CreateGroupScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    uiState: CreateExpenseUiState,
    onBackClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPayerSelected: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSplitModeSelected: (SplitMode) -> Unit,
    onParticipantToggled: (String) -> Unit,
    onPercentageChanged: (participantId: String, percentageInput: String) -> Unit,
    onFixedAmountChanged: (participantId: String, fixedAmountInput: String) -> Unit,
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
    val colors = LocalTallyColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = {
            CreateExpenseTopBar(
                onBackClick = onBackClick,
                isEditMode = uiState.isEditMode,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        },
        bottomBar = {
            TallyBottomBar(
                selectedTab = TallyBottomTab.GROUPS,
                unreadNotificationsCount = unreadNotificationsCount,
                authenticatedUserName = authenticatedUserName,
                onTabSelected = { tab ->
                    when (tab) {
                        TallyBottomTab.GROUPS -> onGroupsClick()
                        TallyBottomTab.NOTIFICATIONS -> onNotificationsClick()
                        TallyBottomTab.PROFILE -> onProfileClick()
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
            DescriptionField(
                description = uiState.description,
                isError = uiState.descriptionError,
                onDescriptionChange = onDescriptionChange,
                modifier = Modifier.padding(top = 12.dp),
            )
            AmountField(
                amountInput = uiState.amountInput,
                isError = uiState.amountError,
                onAmountChange = onAmountChange,
            )
            PayerField(
                participants = uiState.participants,
                selectedPayerId = uiState.payerId,
                onPayerSelected = onPayerSelected,
            )
            DateField(date = uiState.date, onDateSelected = onDateSelected)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "How to split", fontWeight = FontWeight.SemiBold)
                SplitTypeTabs(selectedMode = uiState.splitMode, onModeSelected = onSplitModeSelected)
                when (uiState.splitMode) {
                    SplitMode.EQUAL -> ParticipantSplitList(
                        rows = uiState.splitRows,
                        isError = uiState.participantsError,
                        onParticipantToggled = onParticipantToggled,
                    )

                    SplitMode.PERCENTAGE -> PercentageSplitList(
                        rows = uiState.splitRows,
                        onPercentageChanged = onPercentageChanged,
                    )

                    SplitMode.FIXED_AMOUNT -> FixedAmountSplitList(
                        rows = uiState.splitRows,
                        total = Money.ofCents(parseAmountInputToCents(uiState.amountInput) ?: 0L),
                        onFixedAmountChanged = onFixedAmountChanged,
                    )
                }
            }

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
                Text(
                    text = if (uiState.isEditMode) "Save changes" else "Save expense",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
