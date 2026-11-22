package cn.turingzy.ledger.ui.feature.bill.detail

import cn.turingzy.ledger.ui.feature.bill.sheet.RecordTransactionSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalConfiguration
import cn.turingzy.ledger.R
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

/**
 * 交易详情及编辑弹窗页面。
 *
 * 监听 [TransactionDetailsViewModel] 中选中的交易 ID，加载交易详情；
 * 若交易不存在或加载失败则回调提示，加载成功时展示可编辑的底部抽屉。
 *
 * @param onLoadFailure 加载失败时的提示文案回调。
 * @param modifier 外部修饰符。
 * @param viewModel 交易详情 ViewModel。
 * @param sheetHeight 底部抽屉的固定高度，默认屏幕高度的 80%。
 */
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
        )
    }
}

/**
 * 交易详情编辑抽屉内容组件。
 *
 * 包装 [RecordTransactionSheet]，展示单条交易的全部字段（金额、类型、分类、账户、转账信息、时间、备注等）并支持修改保存。
 *
 * @param uiState 加载成功的交易详情与候选数据 [TransactionDetailsUiState.Success]。
 * @param onBack 关闭或返回回调。
 * @param modifier 外部修饰符。
 * @param saveState 保存状态（保存中、失败错误等）。
 * @param onSave 提交修改后的交易数据及汇率覆盖。
 * @param sheetHeight 抽屉高度。
 * @param onLoadExchangeRate 跨币种动态汇率加载回调。
 */
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
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saveState.saving || it != SheetValue.Hidden },
    )
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
    )
}
