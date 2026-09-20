package com.rateio.app.ui.createexpense

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
import com.rateio.app.ui.format.parseAmountInputToCents
import com.rateio.app.ui.theme.LocalRateioColors
import com.rateio.domain.model.Money
import java.time.LocalDate

/**
 * Tela "Nova despesa" (T24), aberta a partir do botão "+" em "Detalhes do grupo" (T42.2/RF42) —
 * até T42.4 era aberta direto do [com.rateio.app.ui.groups.GroupCard] na lista de grupos, atalho
 * temporário porque a tela de detalhe ainda não existia. `factory` injeta o
 * [CreateExpenseViewModel] pela composição manual de [com.rateio.app.di.AppContainer], mesmo
 * padrão de [com.rateio.app.ui.creategroup.CreateGroupRoute]. `factory.expenseId` não-nulo (T29)
 * é o único gatilho do modo edição — nem esta Route nem [CreateExpenseScreen] decidem isso, só
 * repassam [CreateExpenseUiState.isEditMode] pra pintar título/botão.
 */
@Composable
fun CreateExpenseRoute(
    factory: CreateExpenseViewModelFactory,
    onSaved: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateExpenseViewModel = viewModel(factory = factory)
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
        modifier = modifier,
    )
}

/**
 * Composable fina: só orquestra topbar + campos + lista de participantes + botão — nenhuma delas
 * sabe de onde vem o estado nem faz validação/cálculo (mesmo Object Calisthenics de
 * [com.rateio.app.ui.creategroup.CreateGroupScreen]).
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
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { CreateExpenseTopBar(onBackClick = onBackClick, isEditMode = uiState.isEditMode) },
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
                Text(text = "Como dividir", fontWeight = FontWeight.SemiBold)
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
                    text = if (uiState.isEditMode) "Salvar alterações" else "Salvar despesa",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
