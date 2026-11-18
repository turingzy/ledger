package cn.turingzy.ledger.ui.feature.bill.detail

import cn.turingzy.ledger.ui.feature.bill.sheet.RecordTransactionSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
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
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.Transaction

@Composable
internal fun TransactionDetailsScreen(
    onLoadFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailsViewModel = hiltViewModel(),
    sheetHeight: Dp = LocalConfiguration.current.screenHeightDp.dp * 0.8f,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val selectedId by viewModel.selectedTransactionId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val deleteFailure = stringResource(R.string.ui_transaction_delete_failed)
    LaunchedEffect(selectedId, uiState, saveState.saving, saveState.saved) {
        if (selectedId == null || saveState.saving) return@LaunchedEffect
        if (saveState.saved) {
            viewModel.selectTransaction(null)
            return@LaunchedEffect
        }
        val message = when (val state = uiState) {
            is TransactionDetailsUiState.Error -> R.string.ui_could_not_load_transaction_please_try_again.takeIf { state.transactionId == selectedId }
            is TransactionDetailsUiState.Unavailable -> R.string.ui_transaction_unavailable_or_deleted.takeIf { state.transactionId == selectedId }
            else -> null
        }
        if (message != null) {
            viewModel.selectTransaction(null)
            onLoadFailure(textResources.getString(message))
        }
    }
    if (saveState.saved) return
    val state = uiState as? TransactionDetailsUiState.Success ?: return
    if (state.detail.transaction.id != selectedId) return
    key(selectedId) {
        TransactionDetailsContent(
            state, { viewModel.selectTransaction(null) }, modifier, saveState, viewModel::save, sheetHeight,
            onLoadExchangeRate = viewModel::exchangeRate,
            onDelete = { viewModel.delete(deleteFailure) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDetailsContent(
    uiState: TransactionDetailsUiState.Success,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    saveState: TransactionSaveState = TransactionSaveState(),
    onSave: (Transaction, CurrencyAmountOverride?) -> Unit = { _, _ -> },
    sheetHeight: Dp = 520.dp,
    onLoadExchangeRate: suspend (String, String, Long) -> String? = { _, _, _ -> null },
    onDelete: (() -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saveState.saving || it != SheetValue.Hidden },
    )
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    RecordTransactionSheet(
        sheetState = sheetState,
        sheetHeight = sheetHeight,
        categories = uiState.categories,
        accounts = uiState.accounts,
        onDismissRequest = { if (!saveState.saving) onBack() },
        onLoadExchangeRate = onLoadExchangeRate,
        onConfirm = { type, amount, accountId, toAccountId, categoryId, note, time, toAmount, amountOverride ->
            onSave(uiState.detail.transaction.copy(
                type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
                categoryId = categoryId, note = note, transactionTime = time, toAmount = toAmount,
                transferRate = toAmount?.let { java.math.BigDecimal.valueOf(it).divide(java.math.BigDecimal.valueOf(amount), 18, java.math.RoundingMode.HALF_UP).toPlainString() },
            ), amountOverride)
        },
        modifier = modifier,
        initialTransaction = uiState.detail.transaction,
        title = textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_details),
        saving = saveState.saving,
        error = saveState.error,
        onDelete = onDelete?.let { { confirmDelete = true } },
    )
    if (confirmDelete && onDelete != null) {
        val dismiss = { confirmDelete = false }
        BaseAlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.ui_delete_transaction_title)) },
            text = { Text(stringResource(R.string.ui_delete_transaction_message)) },
            dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.ui_cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    dismiss()
                    onDelete()
                }) {
                    Text(stringResource(R.string.ui_delete_transaction), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
