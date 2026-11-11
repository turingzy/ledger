package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
) {
    val selectedId by viewModel.selectedTransactionId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    LaunchedEffect(selectedId, uiState) {
        if (selectedId == null) return@LaunchedEffect
        when (val state = uiState) {
            is TransactionDetailsUiState.Error -> {
                if (state.transactionId == selectedId) onLoadFailure("交易加载失败，请重试")
            }
            is TransactionDetailsUiState.Unavailable -> {
                if (state.transactionId == selectedId) onLoadFailure("该交易不存在或已删除")
            }
            else -> Unit
        }
    }
    val state = uiState as? TransactionDetailsUiState.Success ?: return
    if (state.detail.transaction.id != selectedId) return
    key(selectedId) {
        TransactionDetailsContent(state, onBack, modifier, saveState, viewModel::save)
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
) {
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
        categories = uiState.categories,
        accounts = uiState.accounts,
        onDismissRequest = { if (!saveState.saving) onBack() },
        onConfirm = { type, amount, accountId, toAccountId, categoryId, note, time ->
            onSave(uiState.detail.transaction.copy(
                type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
                categoryId = categoryId, note = note, transactionTime = time,
            ))
        },
        modifier = modifier,
        initialTransaction = uiState.detail.transaction,
        title = "交易详情",
        saving = saveState.saving,
        error = saveState.error,
    )
}
