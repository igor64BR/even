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
import com.rateio.app.ui.theme.LocalRateioColors
import java.time.LocalDate

/**
 * Tela "Nova despesa" (T24), aberta a partir do [com.rateio.app.ui.groups.GroupCard] na lista de
 * grupos — não existe tela de detalhe de grupo ainda (RF42), mesmo precedente documentado que T19
 * usou pra "Sincronizar este grupo" (ação embutida no próprio card em vez de uma tela dedicada que
 * ainda não existe). `factory` injeta o [CreateExpenseViewModel] pela composição manual de
 * [com.rateio.app.di.AppContainer], mesmo padrão de
 * [com.rateio.app.ui.creategroup.CreateGroupRoute].
 */
@Composable
fun CreateExpenseRoute(
    factory: CreateExpenseViewModelFactory,
    onExpenseCreated: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateExpenseViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateExpenseEvent.ExpenseCreated -> onExpenseCreated()
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
        onParticipantToggled = viewModel::onParticipantToggled,
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
    onParticipantToggled: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { CreateExpenseTopBar(onBackClick = onBackClick) },
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
                SplitTypeTabs()
                ParticipantSplitList(
                    rows = uiState.splitRows,
                    isError = uiState.participantsError,
                    onParticipantToggled = onParticipantToggled,
                )
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
                Text(text = "Salvar despesa", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
