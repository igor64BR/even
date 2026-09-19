package com.rateio.app.ui.settledebts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rateio.app.ui.theme.LocalRateioColors

/**
 * Tela "Quitar dívidas" (T42.3, RF44), aberta a partir do botão homônimo em "Detalhes do grupo"
 * (T42.2). `factory` injeta o [SettleDebtsViewModel] pela composição manual de
 * [com.rateio.app.di.AppContainer], mesmo padrão das demais telas do módulo.
 */
@Composable
fun SettleDebtsRoute(
    factory: SettleDebtsViewModelFactory,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettleDebtsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettleDebtsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onMarkAsPaidClick = viewModel::onMarkAsPaidClick,
        modifier = modifier,
    )
}

/**
 * Composable fina: delega pro corpo certo conforme [uiState] — nenhum cálculo de saldo/settlement
 * mora aqui, [SettleDebtsViewModel] já entrega a lista pronta (Object Calisthenics).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtsScreen(
    uiState: SettleDebtsUiState,
    onBackClick: () -> Unit,
    onMarkAsPaidClick: (SettlementSuggestionRowUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.paper,
        topBar = { SettleDebtsTopBar(onBackClick = onBackClick) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                is SettleDebtsUiState.Loading -> Unit
                is SettleDebtsUiState.SettledUp -> SettledUpState(groupName = uiState.groupName)
                is SettleDebtsUiState.Content -> SettleDebtsContent(
                    suggestions = uiState.suggestions,
                    onMarkAsPaidClick = onMarkAsPaidClick,
                )
            }
        }
    }
}

@Composable
private fun SettleDebtsContent(
    suggestions: List<SettlementSuggestionRowUiModel>,
    onMarkAsPaidClick: (SettlementSuggestionRowUiModel) -> Unit,
) {
    val colors = LocalRateioColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Menor número de transferências pra zerar todos os saldos do grupo.",
            fontSize = 13.sp,
            color = colors.inkSoft,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        suggestions.forEach { suggestion ->
            SettlementSuggestionRow(suggestion = suggestion, onMarkAsPaidClick = onMarkAsPaidClick)
        }
    }
}
