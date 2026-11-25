package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.ui.feature.bill.BillViewModel
import cn.turingzy.ledger.ui.component.base.primaryActionSheetHeight
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 新建记账底部抽屉组件。
 *
 * 包装 [RecordTransactionSheet]，注入可用的分类与账户列表，
 * 支持基于传入的初始基准日期快速录入新交易并提交持久化。
 *
 * @param date 记账默认发生的基准日期。
 * @param onDismiss 关闭或取消抽屉的回调。
 * @param modifier 外部修饰符。
 * @param viewModel 账单 ViewModel，负责创建交易与维护保存状态。
 * @param transactionViewModel 交易详情 ViewModel，用于查询汇率。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordCreateSheet(
    date: LocalDate,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel(),
    transactionViewModel: TransactionDetailsViewModel = hiltViewModel(),
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
    RecordTransactionSheet(
        sheetState = sheetState,
        categories = categories,
        accounts = accounts,
        initialTransactionTime = initialTime,
        onDismissRequest = {
            if (saveState.saved) viewModel.resetSaveState()
            if (!saveState.saving) onDismiss()
        },
        onConfirm = viewModel::createTransaction,
        modifier = modifier,
        saving = saveState.saving,
        dismissAfterSave = saveState.saved,
        error = saveState.error,
        sheetHeight = primaryActionSheetHeight(),
        onLoadExchangeRate = transactionViewModel::exchangeRate,
    )
}
