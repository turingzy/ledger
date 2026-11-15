package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.Transaction

@Composable
internal fun TransactionDetailsScreen(
    onBack: () -> Unit,
    onLoadFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    sheetHeight: Dp = 520.dp,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val selectedId by viewModel.selectedTransactionId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    LaunchedEffect(selectedId, uiState) {
        if (selectedId == null) return@LaunchedEffect
        when (val state = uiState) {
            is TransactionDetailsUiState.Error -> {
                if (state.transactionId == selectedId) onLoadFailure(textResources.getString(cn.turingzy.ledger.R.string.ui_could_not_load_transaction_please_try_again))
            }
            is TransactionDetailsUiState.Unavailable -> {
                if (state.transactionId == selectedId) onLoadFailure(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_unavailable_or_deleted))
            }
            else -> Unit
        }
    }
    val state = uiState as? TransactionDetailsUiState.Success ?: return
    if (state.detail.transaction.id != selectedId) return
    key(selectedId) {
        TransactionDetailsContent(state, onBack, modifier, saveState, viewModel::save, sheetHeight, viewModel::exchangeRate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDetailsContent(
    uiState: TransactionDetailsUiState.Success,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    saveState: TransactionSaveState = TransactionSaveState(),
    onSave: (Transaction) -> Unit = {},
    sheetHeight: Dp = 520.dp,
    onLoadTransferRate: suspend (String, String, Long) -> String? = { _, _, _ -> null },
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saveState.saving || it != SheetValue.Hidden },
    )
    LaunchedEffect(saveState.saved) {
        if (saveState.saved) {
            sheetState.hide()
            onBack()
        }
    }
    RecordTransactionSheet(
        sheetState = sheetState,
        sheetHeight = sheetHeight,
        categories = uiState.categories,
        accounts = uiState.accounts,
        onDismissRequest = { if (!saveState.saving) onBack() },
        onLoadTransferRate = onLoadTransferRate,
        onConfirm = { type, amount, accountId, toAccountId, categoryId, note, time, toAmount ->
            onSave(uiState.detail.transaction.copy(
                type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
                categoryId = categoryId, note = note, transactionTime = time, toAmount = toAmount,
                transferRate = toAmount?.let { java.math.BigDecimal.valueOf(it).divide(java.math.BigDecimal.valueOf(amount), 18, java.math.RoundingMode.HALF_UP).toPlainString() },
            ))
        },
        modifier = modifier,
        initialTransaction = uiState.detail.transaction,
        title = textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_details),
        saving = saveState.saving,
        error = saveState.error,
    )
}
