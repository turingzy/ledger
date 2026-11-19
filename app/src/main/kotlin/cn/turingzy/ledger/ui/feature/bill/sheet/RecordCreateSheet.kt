package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.ui.feature.bill.BillViewModel
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordCreateSheet(
    date: LocalDate,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel(),
    transactionViewModel: TransactionDetailsViewModel = hiltViewModel(),
    draft: Transaction? = null,
) {
    var ready by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        if (!ready) {
            viewModel.resetSaveState()
            ready = true
        }
    }
    if (!ready) return

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saveState.saving || it != SheetValue.Hidden },
    )
    val initialTime = rememberSaveable(date.toEpochDay()) { date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    LaunchedEffect(saveState.saved) {
        if (!saveState.saved) return@LaunchedEffect
        sheetState.hide()
        viewModel.resetSaveState()
        onDismiss()
    }
    RecordTransactionSheet(
        sheetState = sheetState,
        categories = categories,
        accounts = accounts,
        initialTransactionTime = initialTime,
        initialTransaction = draft,
        onDismissRequest = { if (!saveState.saving) onDismiss() },
        onConfirm = viewModel::createTransaction,
        modifier = modifier,
        saving = saveState.saving,
        error = saveState.error,
        sheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f,
        onLoadExchangeRate = transactionViewModel::exchangeRate,
    )
}
